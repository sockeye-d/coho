package dev.fishies.coho.core

import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText
import kotlin.io.path.writeText

class MarkdownFile(val path: Path) : Element(path.name) {
    override fun generate(location: Path) {
        val src = path.readText()
        val flavour = GFMFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(src)
        val html = HtmlGenerator(src, parsedTree, flavour).generateHtml()
        location.resolve(path.nameWithoutExtension + ".html").writeText(html)
    }

    override fun toString() = "$name (${this::class.simpleName} $path)"
}

fun dev.fishies.coho.core.OutputPath.md(source: Path) = children.add(MarkdownFile(source))