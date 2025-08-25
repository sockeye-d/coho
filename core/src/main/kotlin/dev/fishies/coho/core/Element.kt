package dev.fishies.coho.core

import java.nio.file.Path
import java.nio.file.Paths

abstract class Element(val name: String) {
    abstract fun generate(location: Path)
    fun generate(location: String) = generate(Paths.get(location))

    override fun toString() = "$name (${this::class.simpleName})"
}