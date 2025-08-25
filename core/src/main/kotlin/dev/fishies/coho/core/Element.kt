package dev.fishies.coho.core

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

abstract class Element(val name: String) {
    protected abstract fun _generate(location: Path)

    fun generate(location: Path) = location.also { _generate(it) }

    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    fun generate(location: String) = Paths.get(location).also { _generate(it) }!!

    @OptIn(ExperimentalPathApi::class)
    fun forceGenerate(location: Path): Path {
        location.deleteRecursively()
        return generate(location)
    }

    @OptIn(ExperimentalPathApi::class)
    fun forceGenerate(location: String) = forceGenerate(Paths.get(location))

    override fun toString() = "$name (${this::class.simpleName})"
}