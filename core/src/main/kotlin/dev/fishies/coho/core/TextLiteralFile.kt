package dev.fishies.coho.core

import java.nio.file.Path

class TextLiteralFile(name: String, val content: String) : Element(name) {
    override fun _generate(location: Path): List<Path> =
        listOf(location.resolve(name).apply { toFile().writeText(content) })
}

fun OutputPath.text(content: String, name: String = "text") = children.add(TextLiteralFile(name, content))