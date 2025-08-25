package dev.fishies.coho.markdown

import dev.fishies.coho.core.OutputPath
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import java.nio.file.Path

open class ProcessedMarkdownFile(path: Path, val htmlTemplate: ProcessedMarkdownFile.(html: String) -> String) :
    MarkdownFile(path) {
    var frontmatter: Map<String, Any> = emptyMap()
    private fun JsonObject.map(): Map<String, Any> = this.mapValues { (_, value) ->
        when (value) {
            is JsonArray -> toList()
            is JsonObject -> value.map()
            is JsonPrimitive -> when {
                value.isString -> value.content
                value.intOrNull != null -> value.int
                value.doubleOrNull != null -> value.double
                value.booleanOrNull != null -> value.boolean
                else -> Unit
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun preprocessMarkdown(src: String): String {
        if (!src.startsWith("---")) {
            return src
        }
        val nextSeparator = src.indexOf("---", 3)
        if (nextSeparator == -1) {
            return src
        }

        val frontmatterText =
            '{' + src.substring(3, nextSeparator - 1).lines().filter { !it.isBlank() }.joinToString("\n") {
                if (it.endsWith(",") || it.endsWith("{") || it.endsWith("[")) it else "$it,"
            } + '}'

        try {
            frontmatter = Json {
                isLenient = true
                allowComments = true
                allowTrailingComma = true
            }.parseToJsonElement(frontmatterText).jsonObject.map()
        } catch (e: Exception) {
            // if only I could catch JSON-specific exceptions 😔
            println("Failed to parse frontmatter $frontmatterText. Reason: ${e.message}")
        }

        return src.substring(nextSeparator + 3)
    }

    override fun createHtml(src: String, tree: ASTNode, flavour: MarkdownFlavourDescriptor): String {
        val html = HtmlGenerator(src, tree, flavour).generateHtml()
        return htmlTemplate(html)
    }

    companion object {
        var defaultTemplate: ProcessedMarkdownFile.(html: String) -> String =
            { html: String -> "<!DOCTYPE HTML><html>$html</html>" }
    }
}

private fun Any.asMap() = this as Map<*, *>

fun ProcessedMarkdownFile.ogMetadataTemplate(html: String): String {
    val meta = frontmatter["meta"]?.asMap()
    val title = meta?.get("title") as? String
    val description = meta?.get("description") as? String
    val type = meta?.get("type") as? String ?: "website"

    //language=html
    return """
<!DOCTYPE HTML>
<html>
<head>
    <meta charset='UTF-8'>
    ${if (title != null) /* language=html */ "<title>$title</title>" else ""}
    ${if (title != null) /* language=html */ "<meta property='og:title' content='$title'>" else ""}
    ${if (description != null) /* language=html */ "<meta property='og:description' content='$description'>" else ""}
    <meta property='og:type' content='$type'>
</head>
<body>
${html.prependIndent()}
</body>
</html>
    """.trimIndent()
}

fun OutputPath.md(
    source: Path,
    htmlTemplate: ProcessedMarkdownFile.(html: String) -> String = ProcessedMarkdownFile.defaultTemplate,
) =
    children.add(ProcessedMarkdownFile(source, htmlTemplate))