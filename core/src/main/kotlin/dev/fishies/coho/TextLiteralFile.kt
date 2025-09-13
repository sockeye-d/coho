package dev.fishies.coho

import java.nio.file.Path

/**
 * @suppress
 */
class TextLiteralFile(destination: String, val content: String) : Element(destination) {
    override fun _generate(location: Path): List<Path> =
        listOf(location.resolve(name).apply { toFile().writeText(content) })
}

/**
 * Output [content] to [destination].
 *
 * Useful for small text.
 */
fun OutputPath.text(content: String, destination: String) =
    children.add(anonymous(name = destination) { listOf(it.resolve(destination).apply { toFile().writeText(content) }) })