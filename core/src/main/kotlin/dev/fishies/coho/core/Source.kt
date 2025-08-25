package dev.fishies.coho.core

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

class Source(val sourcePath: Path) {
    constructor(path: String) : this(Paths.get(path))

    fun text(filename: String) = sourcePath.resolve(filename).readText()

    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    fun path(filename: String) = sourcePath.resolve(filename)!!

    fun cd(dir: String) = Source(sourcePath.resolve(dir))
}