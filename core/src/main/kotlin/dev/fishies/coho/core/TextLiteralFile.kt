package dev.fishies.coho.core

import java.nio.file.Path

class TextLiteralFile(name: String, val content: String): Element(name) {
    override fun generate(location: Path) {
        location.resolve(name).toFile().writeText(content)
    }
}