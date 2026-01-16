package dev.fishies.coho

import net.mamoe.yamlkt.YamlNull.content
import java.nio.file.Path

/**
 * @suppress
 */
class TextLiteralFile(destination: String, val content: () -> String) : Element(destination) {
    override fun _generate(location: Path): List<Path> =
        listOf(location.resolve(name).apply { toFile().writeText(content()) })
}

/**
 * Output [content] to [destination].
 *
 * Useful for small text.
 */
fun OutputPath.text(destination: String, content: () -> String) =
    children.add(TextLiteralFile(destination, content))