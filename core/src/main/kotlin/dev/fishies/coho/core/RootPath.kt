package dev.fishies.coho.core

import java.nio.file.Path
import kotlin.io.path.createDirectory

class RootPath(sourceDirectory: Source) : OutputPath("root", sourceDirectory) {
    override fun generate(location: Path) {
        val contentPath = location
        contentPath.createDirectory()
        for (child in children) {
            child.generate(contentPath)
        }
    }
}

fun root(sourceDirectory: Source, block: RootPath.() -> Unit) = RootPath(sourceDirectory).apply { block() }
fun root(sourceDirectory: String, block: RootPath.() -> Unit) = root(Source(sourceDirectory), block)
