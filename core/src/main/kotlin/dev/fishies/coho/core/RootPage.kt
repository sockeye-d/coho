package dev.fishies.coho.core

import java.nio.file.Path
import kotlin.io.path.createDirectory

class RootPage(sourceDirectory: Source) : Page("root", sourceDirectory) {
    override fun generate(location: Path) {
        val contentPath = location
        contentPath.createDirectory()
        for (child in children) {
            child.generate(contentPath)
        }
    }
}

fun root(sourceDirectory: Source, block: RootPage.() -> Unit) = RootPage(sourceDirectory).apply { block() }
