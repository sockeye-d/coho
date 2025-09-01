package dev.fishies.coho.core

import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchKey
import java.nio.file.WatchService
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.io.path.*
import kotlin.time.TimeSource

class RootPath(sourceDirectory: Source, buildPath: Path) : OutputPath("root", sourceDirectory, buildPath, { html: String -> "<!DOCTYPE HTML><html>$html</html>" }) {
    override fun _generate(location: Path): List<Path> {
        location.createDirectory()
        for (child in children) {
            child.generate(location)
        }
        return emptyList()
    }

    @OptIn(ExperimentalPathApi::class)
    fun forceGenerate(location: Path = buildPath): Path {
        location.deleteRecursively()
        doneCount = 0
        return generate(location)
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun watch(ignorePaths: Set<Path>, exit: AtomicBoolean, rebuild: () -> Boolean) {
        val watcher = source.sourcePath.fileSystem.newWatchService()
        val keys = mutableMapOf<WatchKey, Path>()

        val watchDir: (Path) -> Unit = {
            keys[it.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)] = it
        }

        fun walkDir(dir: Path) {
            watchDir(dir)
            dir.listDirectoryEntries().filter {
                it.isDirectory() && !it.isHidden() && it.normalize() !in ignorePaths
            }.forEach {
                walkDir(it)
            }
        }

        walkDir(source.sourcePath)

        watchLoop@while (true) {
            // val key = watcher.getKey { return }
            var key: WatchKey? = null
            while (key == null) {
                // delay(50.milliseconds)
                Thread.sleep(50)
                if (exit.load()) break@watchLoop
                key = watcher.poll()
            }
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

                if (fullPath.normalize() in ignorePaths) {
                    continue
                }

                info(
                    "$fullPath ${
                        when (kind) {
                            ENTRY_CREATE -> "created"
                            ENTRY_MODIFY -> "modified"
                            ENTRY_DELETE -> "delete"
                            else -> "???"
                        }
                    }, rebuilding"
                )

                val startTime = TimeSource.Monotonic.markNow()
                if (rebuild()) {
                    pos("Rebuild complete in ${startTime.elapsedNow()}")
                } else {
                    err("Rebuild failed")
                }

                if (kind == ENTRY_CREATE && fullPath.isDirectory()) {
                    watchDir(fullPath)
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

    private inline fun WatchService.getKey(elseBlock: () -> Nothing): WatchKey {
        try {
            return take()
        } catch (_: InterruptedException) {
            elseBlock()
        }
    }

    companion object {
        lateinit var rootBuildPath: Path
    }
}

fun root(sourceDirectory: Source, block: RootPath.() -> Unit) = RootPath(sourceDirectory, RootPath.rootBuildPath).apply { block() }
fun root(sourceDirectory: String = ".", block: RootPath.() -> Unit) = root(Source(sourceDirectory), block)
