package dev.fishies.coho.markdown

import dev.fishies.coho.OutputPath
import dev.fishies.coho.core.err
import dev.fishies.coho.escapeXml
import dev.fishies.coho.parseMarkdownFrontmatter
import net.mamoe.yamlkt.Yaml
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import java.nio.file.Path

typealias MarkdownTemplate = ProcessedMarkdownFile.(html: String) -> String
typealias StatefulAttributeCustomizer = ProcessedMarkdownFile.(node: ASTNode, tagName: CharSequence, attributes: Iterable<CharSequence?>) -> Iterable<CharSequence?>

private val hrefFixingRegex = Regex("href=\"([^\"]+)\\.md\"")
private val whitespaceRegex = Regex("\\s+")

/**
 * Attribute customizer that remaps hrefs in links to point to HTML files instead of Markdown files.
 */
fun ProcessedMarkdownFile.hrefFixingAttributesCustomizer(
    node: ASTNode, tagName: CharSequence, attributes: Iterable<CharSequence?>
) = if (tagName == "a") attributes.map { it?.replace(hrefFixingRegex, $$"href=\"$1.html\"") } else attributes

fun ProcessedMarkdownFile.headingIDAttributeCustomizer(
    node: ASTNode, tagName: CharSequence, attributes: Iterable<CharSequence?>
) = if (tagName in listOf("h1", "h2", "h3", "h4", "h5", "h6")) attributes + "id=\"${
    node.children.last().getTextInNode(content).trim().replace(whitespaceRegex, "-").lowercase().escapeXml()
}\"" else attributes

/**
 * Meta-attribute customizer that applies a series of customizers.
 */
fun compoundAttributeCustomizer(vararg customizers: StatefulAttributeCustomizer): StatefulAttributeCustomizer =
    { node: ASTNode, tagName: CharSequence, attributes: Iterable<CharSequence?> ->
        customizers.fold(attributes) { fold, value -> this.value(node, tagName, fold) }
    }

open class ProcessedMarkdownFile(
    content: String,
    path: Path,
    val markdownTemplate: MarkdownTemplate,
    val attributesCustomizer: StatefulAttributeCustomizer
) : MarkdownFile(
    content, path, HtmlGenerator.DefaultTagRenderer({ _, _, it -> it }, false)
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
        val (newFrontmatter, newSrc) = parseMarkdownFrontmatter(src)
        frontmatter = newFrontmatter ?: emptyMap()
        return newSrc
    }

    override fun createHtml(src: String, tree: ASTNode, flavour: MarkdownFlavourDescriptor): String {
        val realTagRenderer = HtmlGenerator.DefaultTagRenderer({ a, b, c -> attributesCustomizer(a, b, c) }, false)
        val html = HtmlGenerator(src, tree, flavour).generateHtml(realTagRenderer)
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
 * By default, [hrefFixingAttributesCustomizer] to retarget hrefs to html files and [headingIDAttributeCustomizer] to
 * add IDs to headings are applied.
 *
 * [MarkdownTemplate]s are functions that accept the HTML output of the Markdown parser and return more HTML.
 * It also includes the context of the [ProcessedMarkdownFile],
 * including [ProcessedMarkdownFile.frontmatter].
 */
fun OutputPath.md(
    source: Path,
    markdownTemplate: MarkdownTemplate = this.markdownTemplate,
    attributesCustomizer: StatefulAttributeCustomizer = compoundAttributeCustomizer(
        ProcessedMarkdownFile::hrefFixingAttributesCustomizer, ProcessedMarkdownFile::headingIDAttributeCustomizer
    ),
) = children.add(ProcessedMarkdownFile("", source, markdownTemplate, attributesCustomizer))