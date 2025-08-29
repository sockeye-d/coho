package dev.fishies.coho.core.markdown

import dev.fishies.coho.core.Element
import dev.fishies.coho.core.OutputPath
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText
import kotlin.io.path.writeText

open class MarkdownFile(val path: Path) : Element(path.name) {
    protected open fun preprocessMarkdown(src: String) = src
    protected open fun createHtml(src: String, tree: ASTNode, flavour: MarkdownFlavourDescriptor) =
        HtmlGenerator(src, tree, flavour).generateHtml()

    override fun _generate(location: Path): List<Path> {
        val src = preprocessMarkdown(path.readText())
        val flavour = CommonMarkFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(src)
        return listOf(
            location.resolve(path.nameWithoutExtension + ".html")
                .apply { writeText(createHtml(src, parsedTree, flavour)) })
    }

    override fun toString() = "$name (${this::class.simpleName} $path)"
}

fun OutputPath.mdBasic(source: Path) = children.add(MarkdownFile(source))