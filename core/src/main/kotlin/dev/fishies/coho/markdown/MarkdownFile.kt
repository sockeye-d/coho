package dev.fishies.coho.markdown

import dev.fishies.coho.*
import org.intellij.markdown.*
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.*
import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser
import java.nio.file.Path
import java.util.regex.Pattern.compile
import kotlin.io.path.*

private val shebangRegex = compile("(?<=^#!)\\w+")
private val highlightedCodeSpanProvider = object : GeneratingProvider {
    override fun processNode(
        visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode
    ) {
        val nodes = node.children.subList(1, node.children.size - 1)
        val output =
            nodes.joinToString(separator = "") { HtmlGenerator.leafText(text, it, false) }.trim().unescapeHtml()
        val shebangMatcher = shebangRegex.matcher(output)
        visitor.consumeTagOpen(node, "span", "class=\"inline-code\"")
        visitor.consumeTagOpen(node, "code")
        visitor.consumeHtml(
            if (shebangMatcher.find()) {
                val language: String = shebangMatcher.group()
                output.substring(2 + language.length).trim().highlight(language) ?: "error"
            } else output
        )
        visitor.consumeTagClose("code")
        visitor.consumeTagClose("span")
    }
}
private val highlightedCodeFenceProvider = object : GeneratingProvider {
    override fun processNode(
        visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode
    ) {
        val indentBefore = node.getTextInNode(text).commonPrefixWith(" ".repeat(10)).length

        visitor.consumeHtml("<pre class=\"codeblock\">")

        var state = 0

        var childrenToConsider = node.children
        if (childrenToConsider.last().type == MarkdownTokenTypes.CODE_FENCE_END) {
            childrenToConsider = childrenToConsider.subList(0, childrenToConsider.size - 1)
        }

        var lastChildWasContent = false

        val attributes = ArrayList<String>()
        val content = StringBuilder()
        var language: String? = null
        for (child in childrenToConsider) {
            if (state == 1 && child.type in listOf(
                    MarkdownTokenTypes.CODE_FENCE_CONTENT, MarkdownTokenTypes.EOL
                )
            ) {
                content.append(
                    HtmlGenerator.trimIndents(
                        HtmlGenerator.leafText(text, child, false), indentBefore
                    ).toString().unescapeHtml()
                )
                lastChildWasContent = child.type == MarkdownTokenTypes.CODE_FENCE_CONTENT
            }
            if (state == 0 && child.type == MarkdownTokenTypes.FENCE_LANG) {
                language = HtmlGenerator.leafText(text, child).toString().trim().split(' ')[0]
                attributes.add(
                    "class=\"language-$language\""
                )
            }
            if (state == 0 && child.type == MarkdownTokenTypes.EOL) {
                visitor.consumeTagOpen(node, "code", *attributes.toTypedArray())
                state = 1
            }
        }
        val highlighted = language?.run { content.toString().highlight(this) }
        visitor.consumeHtml(highlighted ?: content.toString().escapeHtml())

        if (state == 0) {
            visitor.consumeTagOpen(node, "code", *attributes.toTypedArray())
        }
        if (lastChildWasContent) {
            visitor.consumeHtml("\n")
        }
        visitor.consumeHtml("</code></pre>")
    }
}

/**
 * Hacked together Markdown flavor descriptor to highlight codeblocks with Prism4j.
 */
open class SyntaxHighlightedGFMFlavourDescriptor(
    useSafeLinks: Boolean = true, absolutizeAnchorLinks: Boolean = false
) : GFMFlavourDescriptor(useSafeLinks, absolutizeAnchorLinks) {
    override fun createHtmlGeneratingProviders(linkMap: LinkMap, baseURI: URI?): Map<IElementType, GeneratingProvider> {
        val base = super.createHtmlGeneratingProviders(linkMap, baseURI)
        return base + mapOf(
            MarkdownElementTypes.CODE_SPAN to highlightedCodeSpanProvider,
            MarkdownElementTypes.CODE_FENCE to highlightedCodeFenceProvider
        )
    }
}

/**
 * @suppress
 */
open class MarkdownFile(var content: String, val path: Path, var tagRenderer: HtmlGenerator.TagRenderer) : Element(path.name) {
    protected open fun preprocessMarkdown(src: String) = src
    protected open fun createHtml(src: String, tree: ASTNode, flavour: MarkdownFlavourDescriptor) =
        HtmlGenerator(src, tree, flavour).generateHtml(tagRenderer)

    override fun _generate(location: Path): List<Path> {
        content = preprocessMarkdown(path.readText())
        val flavour = SyntaxHighlightedGFMFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(content)
        return listOf(
            location.resolve(path.nameWithoutExtension + ".html")
                .apply { writeText(createHtml(content, parsedTree, flavour)) })
    }
}

/**
 * Convert a Markdown file to an HTML body directly with no templating or other post-processing.
 * You likely don't want this one, use [md] for more flexibility.
 */
fun OutputPath.mdBasic(source: Path) = children.add(
    MarkdownFile(
        "", source, HtmlGenerator.DefaultTagRenderer(
            DUMMY_ATTRIBUTES_CUSTOMIZER, false
        )
    )
)