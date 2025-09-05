package dev.fishies.coho.core

import dev.fishies.coho.core.html.html
import dev.fishies.coho.core.markdown.MarkdownTemplate
import dev.fishies.coho.core.markdown.md
import java.nio.file.Path
import kotlin.io.path.createDirectory

/**
 * Represents a subdirectory in the output directory.
 *
 * Arbitrary [Element]s,
 * like [md] and [html], can be nested within an
 * [OutputPath] to specify the contents of that directory.
 */
open class OutputPath(name: String, val source: Source, val buildPath: Path, var markdownTemplate: MarkdownTemplate) :
    Element(name) {
    val children = mutableListOf<Element>()

    override fun _generate(location: Path): List<Path> {
        val contentPath = location.resolve(name)
        contentPath.createDirectory()
        for (child in children) {
            child.generate(contentPath)
        }
        return emptyList()
    }

    override fun toString() =
        "${super.toString()}\n${children.sortedBy { executionTime }.joinToString("\n").prependIndent()}"

    operator fun String.unaryPlus() = source.path(this)
    fun src(path: String) = source.path(path)
    fun build(path: String): Path = buildPath.resolve(path)

    override val count: Int
        get() = children.sumOf { it.count }
}

/**
 * Represents a subdirectory in the output directory.
 *
 * Arbitrary [Element]s,
 * like [md] and [html], can be nested within an
 * [OutputPath] to specify the contents of that directory.
 */
fun OutputPath.path(
    name: String, markdownTemplate: MarkdownTemplate = this.markdownTemplate, block: OutputPath.() -> Unit
) = children.add(OutputPath(name, source.cd(name), buildPath.resolve(name), markdownTemplate).apply { block() })

/**
 * Run [block] at build time.
 */
fun OutputPath.run(name: String = "run", block: OutputPath.(location: Path) -> List<Path>) =
    children.add(object : Element(name) {
        override fun _generate(location: Path): List<Path> {
            return block(location)
        }
    })