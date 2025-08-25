package dev.fishies.coho.core

import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.name

class CopyFile(val path: Path) : Element(path.name) {
    override fun _generate(location: Path) {
        path.copyTo(location.resolve(name))
    }

    override fun toString() = "$name (${this::class.simpleName} $path)"
}

fun OutputPath.cp(source: Path) = children.add(CopyFile(source))