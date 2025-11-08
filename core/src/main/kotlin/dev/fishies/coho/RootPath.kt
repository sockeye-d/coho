package dev.fishies.coho

import java.nio.file.Path
import kotlin.io.path.*

/**
 * @suppress
 */
class RootPath(sourceDirectory: Source, buildPath: Path) : OutputPath(
    "root", sourceDirectory, buildPath, { html: String -> "<!DOCTYPE HTML><html>$html</html>" }, emptyList()
) {
    /**
     * The passed command line arguments. Useful for customizing build behavior.
     */
    val arguments
        get() = RootPath.arguments

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

    companion object {
        lateinit var rootBuildPath: Path
        lateinit var scriptSourcePath: Path
        lateinit var arguments: Map<String, List<String>>
    }
}

/**
 * Represents the root directory.
 * Use [path] to define subdirectories.
 */
fun root(sourceDirectory: Source, block: RootPath.() -> Unit) =
    RootPath(sourceDirectory, RootPath.rootBuildPath).apply { block() }

/**
 * Represents the root directory.
 * Use [path] to define subdirectories.
 */
fun root(sourceDirectory: String = ".", block: RootPath.() -> Unit) =
    root(Source(sourceDirectory, RootPath.scriptSourcePath.resolve("..").normalize()), block)
