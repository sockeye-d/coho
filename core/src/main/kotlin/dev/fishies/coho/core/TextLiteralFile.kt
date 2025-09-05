package dev.fishies.coho.core

import java.nio.file.Path

class TextLiteralFile(destination: String, val content: String) : Element(destination) {
    override fun _generate(location: Path): List<Path> =
        listOf(location.resolve(name).apply { toFile().writeText(content) })
}

/**
 * Output [content] to [destination].
 * Useful for small text.
 */
fun OutputPath.text(content: String, destination: String = "text") = children.add(TextLiteralFile(destination, content))