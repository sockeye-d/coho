package dev.fishies.coho.core

import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.name

open class CopyFile(val path: Path, destName: String) : Element(destName) {
    override fun _generate(location: Path): List<Path> = listOf(location.resolve(name).also { path.copyTo(it) })
}

/**
 * Copies [source] to the output with the name [destName].
 */
fun OutputPath.cp(source: Path, destName: String = source.name) = children.add(CopyFile(source, destName))