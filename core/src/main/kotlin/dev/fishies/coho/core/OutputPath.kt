package dev.fishies.coho.core

import org.jetbrains.annotations.Contract
import java.nio.file.Path
import kotlin.io.path.createDirectory

open class OutputPath(name: String, val source: Source, val buildPath: Path) : Element(name) {
    val children = mutableListOf<Element>()

    override fun _generate(location: Path): List<Path> {
        val contentPath = location.resolve(name)
        contentPath.createDirectory()
        for (child in children) {
            child.generate(contentPath)
        }
        return emptyList()
    }

    override fun toString() = "$name (${this::class.simpleName})\n${children.joinToString("\n").prependIndent()}"

    operator fun String.unaryPlus() = source.path(this)
    fun src(path: String) = source.path(path)
    fun build(path: String): Path = buildPath.resolve(path)

    override val count: Int
        get() = children.size + 1
}

fun OutputPath.path(name: String, block: OutputPath.() -> Unit) =
    children.add(OutputPath(name, source.cd(name), buildPath.resolve(name)).apply { block() })

inline fun OutputPath.run(crossinline block: OutputPath.(location: Path) -> List<Path>) = children.add(object : Element("run") {
    override fun _generate(location: Path): List<Path> {
        return block(location)
    }
})