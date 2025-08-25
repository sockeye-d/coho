package dev.fishies.coho.core

import java.awt.event.FocusEvent
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

abstract class Element(val name: String) {
    abstract fun generate(location: Path)
    fun generate(location: String) = generate(Paths.get(location))

    @OptIn(ExperimentalPathApi::class)
    fun forceGenerate(location: Path) {
        location.deleteRecursively()
        generate(location)
    }

    @OptIn(ExperimentalPathApi::class)
    fun forceGenerate(location: String) = forceGenerate(Paths.get(location))

    override fun toString() = "$name (${this::class.simpleName})"
}