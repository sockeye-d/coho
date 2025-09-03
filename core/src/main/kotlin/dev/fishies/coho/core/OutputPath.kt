package dev.fishies.coho.core

import dev.fishies.coho.core.markdown.MarkdownTemplate
import java.nio.file.Path
import kotlin.io.path.createDirectory

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

    override fun toString() = "${super.toString()}\n${children.sortedBy { executionTime }.joinToString("\n").prependIndent()}"

    operator fun String.unaryPlus() = source.path(this)
    fun src(path: String) = source.path(path)
    fun build(path: String): Path = buildPath.resolve(path)

    override val count: Int
        get() = children.sumOf { it.count }
}

fun OutputPath.path(
    name: String, markdownTemplate: MarkdownTemplate = this.markdownTemplate, block: OutputPath.() -> Unit
) = children.add(OutputPath(name, source.cd(name), buildPath.resolve(name), markdownTemplate).apply { block() })

inline fun OutputPath.run(name: String = "run", crossinline block: OutputPath.(location: Path) -> List<Path>) =
    children.add(object : Element(name) {
        override fun _generate(location: Path): List<Path> {
            return block(location)
        }
    })