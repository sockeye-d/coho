package dev.fishies.coho.core

import java.nio.file.Path
import kotlin.io.path.createDirectory

open class OutputPath(name: String, val src: Source) : Element(name) {
    val children = mutableListOf<Element>()

    override fun _generate(location: Path) {
        val contentPath = location.resolve(name)
        contentPath.createDirectory()
        for (child in children) {
            child.generate(contentPath)
        }
    }

    override fun toString() = "$name (${this::class.simpleName})\n${children.joinToString("\n").prependIndent()}"

    operator fun String.unaryPlus() = src.path(this)
}

fun dev.fishies.coho.core.OutputPath.path(name: String, block: dev.fishies.coho.core.OutputPath.() -> Unit) =
    children.add(OutputPath(name, src.cd(name)).apply { block() })
