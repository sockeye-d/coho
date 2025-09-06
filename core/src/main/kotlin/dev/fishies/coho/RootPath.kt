package dev.fishies.coho

import dev.fishies.coho.core.err
import dev.fishies.coho.core.info
import dev.fishies.coho.core.pos
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchKey
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.io.path.*
import kotlin.time.TimeSource

/**
 * @suppress
 */
class RootPath(sourceDirectory: Source, buildPath: Path) : OutputPath("root", sourceDirectory, buildPath, { html: String -> "<!DOCTYPE HTML><html>$html</html>" }) {
    override fun _generate(location: Path): List<Path> {
        location.createDirectory()
        for (child in children) {
            child.generate(location)
        }
        return emptyList()
    }

    @OptIn(ExperimentalPathApi::class)
    override fun generate(location: Path?): Path {
        val realLocation = location ?: buildPath
        realLocation.deleteRecursively()
        doneCount = 0
        maxCount = count
        val result = super.generate(realLocation)
        if (doneCount < maxCount) {
            maxCount = count
        }
        return result
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

    companion object {
        lateinit var rootBuildPath: Path
    }
}

/**
 * Represents the root directory.
 * Use [path] to define subdirectories.
 */
fun root(sourceDirectory: Source, block: RootPath.() -> Unit) = RootPath(sourceDirectory, RootPath.rootBuildPath).apply { block() }

/**
 * Represents the root directory.
 * Use [path] to define subdirectories.
 */
fun root(sourceDirectory: String = ".", block: RootPath.() -> Unit) = root(Source(sourceDirectory), block)
