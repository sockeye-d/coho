package dev.fishies.coho.core.markdown

import dev.fishies.coho.core.OutputPath
import dev.fishies.coho.core.err
import kotlinx.serialization.ExperimentalSerializationApi
import net.mamoe.yamlkt.Yaml
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import java.nio.file.Path
import kotlin.collections.get

typealias MarkdownTemplate = ProcessedMarkdownFile.(html: String) -> String

open class ProcessedMarkdownFile(path: Path, val markdownTemplate: MarkdownTemplate) : MarkdownFile(path) {
    var frontmatter: Map<String?, Any?> = emptyMap()

    @OptIn(ExperimentalSerializationApi::class)
    override fun preprocessMarkdown(src: String): String {
        if (!src.startsWith("```")) {
            return src
        }
        val startIndex = if (src.startsWith("```yaml")) 7 else 3
        val nextSeparator = src.indexOf("```", 3)
        if (nextSeparator == -1) {
            return src
        }

        val frontmatterText = src.substring(startIndex, nextSeparator - 1)
        try {
            frontmatter = Yaml.decodeMapFromString(frontmatterText)
        } catch (e: IllegalArgumentException) {
            err("Failed to parse frontmatter $frontmatterText. Reason: ${e.message}")
        }

        return src.substring(nextSeparator + 4)
    }

    override fun createHtml(src: String, tree: ASTNode, flavour: MarkdownFlavourDescriptor): String {
        val html = HtmlGenerator(src, tree, flavour).generateHtml()
        return markdownTemplate(html)
    }
}

fun Any.asMap() = this as Map<*, *>

inline operator fun <K, reified O> Map<out Any?, *>?.get(key: K) = this?.get(key) as? O

fun ProcessedMarkdownFile.ogMetadataTemplate(html: String): String {
    val meta = frontmatter["meta"]?.asMap()
    val title: String? = meta["title"]
    val description: String? = meta["description"]
    val type: String? = meta["type"]

    // language=html
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
    markdownTemplate: MarkdownTemplate = this.markdownTemplate,
) = children.add(ProcessedMarkdownFile(source, markdownTemplate))