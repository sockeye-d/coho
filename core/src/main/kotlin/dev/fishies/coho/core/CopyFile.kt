package dev.fishies.coho.core

import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.name

open class CopyFile(val path: Path) : Element(path.name) {
    override fun _generate(location: Path): List<Path> = listOf(location.resolve(name).also { path.copyTo(it) })
}

fun OutputPath.cp(source: Path) = children.add(CopyFile(source))