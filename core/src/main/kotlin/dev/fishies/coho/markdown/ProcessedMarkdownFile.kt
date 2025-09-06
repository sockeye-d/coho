package dev.fishies.coho.markdown

import dev.fishies.coho.OutputPath
import dev.fishies.coho.core.err
import net.mamoe.yamlkt.Yaml
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.html.AttributesCustomizer
import org.intellij.markdown.html.HtmlGenerator
import java.nio.file.Path

typealias MarkdownTemplate = ProcessedMarkdownFile.(html: String) -> String

private val hrefFixingRegex = Regex("href=\"([^\"]+)\\.md\"")

/**
 * Attribute customizer that remaps hrefs in links to point to HTML files instead of Markdown files.
 */
fun hrefFixingAttributesCustomizer(
    node: ASTNode, tagName: CharSequence, attributes: Iterable<CharSequence?>
) = if (tagName == "a") attributes.map { it?.replace(hrefFixingRegex, $$"href=\"$1.html\"") } else attributes

open class ProcessedMarkdownFile(
    path: Path, val markdownTemplate: MarkdownTemplate, attributesCustomizer: AttributesCustomizer
) : MarkdownFile(
    path, HtmlGenerator.DefaultTagRenderer(attributesCustomizer, false)
) {
    /**
     * The 'frontmatter' of the Markdown document; e.g., the section that comes before the content:
     *
     * ````md
     * ```yaml
     * meta:
     *   title: a good title
     * ```
     *
     * # Page
     *
     * content
     * ````
     *
     * It's always parsed as YAML, although the codeblock only optionally includes the `yaml` language specifier.
     */
    var frontmatter: Map<String?, Any?> = emptyMap()

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
        val html = HtmlGenerator(src, tree, flavour).generateHtml(tagRenderer)
        return markdownTemplate(html)
    }
}

/**
 * Convert [Any] to a [Map] in a way that's easily null-safetied.
 */
fun Any.asMap() = this as Map<*, *>

/**
 * A default template for [md] that adds [OpenGraph](https://ogp.me/) [`meta`](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/meta)
 * tags to the HTML.
 */
fun ProcessedMarkdownFile.ogMetadataTemplate(html: String): String {
    val meta = frontmatter["meta"]?.asMap()
    val title: String? = meta?.get("title") as? String
    val description: String? = meta?.get("description") as? String
    val type: String? = meta?.get("type") as? String

    frontmatter["meta"] // language=html
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

/**
 * Convert a Markdown file to an HTML body with the given template and attribute customizer.
 *
 * [MarkdownTemplate]s are functions that accept the HTML output of the Markdown parser and return more HTML.
 * It also includes the context of the [ProcessedMarkdownFile],
 * including [ProcessedMarkdownFile.frontmatter].
 */
fun OutputPath.md(
    source: Path,
    markdownTemplate: MarkdownTemplate = this.markdownTemplate,
    attributesCustomizer: AttributesCustomizer = ::hrefFixingAttributesCustomizer,
) = children.add(ProcessedMarkdownFile(source, markdownTemplate, attributesCustomizer))