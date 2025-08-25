package dev.fishies.coho.core

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchKey
import java.nio.file.WatchService
import kotlin.io.path.createDirectory
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden


class RootPath(sourceDirectory: Source) : OutputPath("root", sourceDirectory) {
    override fun _generate(location: Path) {
        val contentPath = location
        contentPath.createDirectory()
        for (child in children) {
            child.generate(contentPath)
        }
    }

    fun watch(rebuild: () -> Unit) {
        val watcher = src.sourcePath.fileSystem.newWatchService()
        val keys = mutableMapOf<WatchKey, Path>()

        fun registerDirectory(dir: Path) {
            keys[dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)] = dir
            println("Registered $dir")
        }

        Files.walk(src.sourcePath).filter { it.isDirectory() }.forEach { registerDirectory(it) }

        while (true) {
            val key = getKey(watcher) { return }
            val dir = keys[key] ?: error("hi")

            for (event in key.pollEvents()) {
                val kind = event.kind()

                if (kind == OVERFLOW) {
                    continue
                }

                val filename = event.context() as Path
                val fullPath = dir.resolve(filename)

                if (fullPath.isHidden()) {
                    continue
                }

                println(
                    "$fullPath ${
                        when (kind) {
                            ENTRY_CREATE -> "created"
                            ENTRY_MODIFY -> "modified"
                            ENTRY_DELETE -> "delete"
                            else -> "???"
                        }
                    }"
                )

                rebuild()

                println("Rebuild complete")

                if (kind == ENTRY_CREATE && fullPath.isDirectory()) {
                    registerDirectory(fullPath)
                }
            }

            val valid = key.reset()
            if (!valid) {
                keys.remove(key)
                if (keys.isEmpty()) break
            }
        }

        watcher.close()
    }

    private inline fun getKey(watcher: WatchService, elseBlock: () -> Nothing): WatchKey {
        try {
            return watcher.take()
        } catch (e: InterruptedException) {
            elseBlock()
        }
    }
}

fun root(sourceDirectory: Source, block: RootPath.() -> Unit) = RootPath(sourceDirectory).apply { block() }
fun root(sourceDirectory: String, block: RootPath.() -> Unit) = root(Source(sourceDirectory), block)
