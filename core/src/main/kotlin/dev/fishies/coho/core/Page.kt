package dev.fishies.coho.core

import java.nio.file.Path
import kotlin.io.path.createDirectory

open class Page(name: String, val src: Source) : Element(name) {
    internal val children = mutableListOf<Element>()

    override fun generate(location: Path) {
        val contentPath = location.resolve(name)
        contentPath.createDirectory()
        for (child in children) {
            child.generate(contentPath)
        }
    }

    override fun toString() = "$name (${this::class.simpleName})\n${children.joinToString("\n").prependIndent()}"

    operator fun String.unaryPlus() = src.path(this)
}

fun Page.page(name: String, block: Page.() -> Unit) =
    children.add(Page(name, src.cd(name)).apply { block() })
