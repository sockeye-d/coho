package dev.fishies.coho.core

import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.name

class HTMLFile(val path: Path) : Element(path.name) {
    override fun generate(location: Path) {
        path.copyTo(location.resolve(name))
    }

    override fun toString() = "$name (${this::class.simpleName} $path)"
}

fun Page.html(source: Path) = children.add(HTMLFile(source))