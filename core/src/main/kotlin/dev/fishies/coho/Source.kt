package dev.fishies.coho

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

class Source(val sourcePath: Path) : Iterable<Path> {
    constructor(path: String, relativeTo: Path) : this(relativeTo.resolve(path))

    fun text(filename: String) = sourcePath.resolve(filename).readText()

    fun path(filename: String): Path = sourcePath.resolve(filename)

    fun cd(dir: String) = Source(sourcePath.resolve(dir))

    fun entries(pattern: String? = null): List<Path> = sourcePath.listDirectoryEntries(pattern ?: "*")
    fun files(pattern: String? = null): List<Path> = entries(pattern).filter { !it.isDirectory() }
    fun dirs(pattern: String? = null): List<Path> = entries(pattern).filter { it.isDirectory() }

    override fun iterator(): Iterator<Path> = entries().iterator()
}