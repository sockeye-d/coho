package dev.fishies.coho.cli

import dev.fishies.coho.core.RootPath
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.default
import java.nio.file.Path
import java.nio.file.Paths
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyTo
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readText

object ExistingPathArgType : ArgType<Path>(true) {
    override val description: kotlin.String
        get() = "{ Path (must exist) }"

    override fun convert(value: kotlin.String, name: kotlin.String): Path {
        val path = Paths.get(value)
        if (!path.exists()) {
            error("Path $path does not exist.")
        }
        return path
    }
}

object PathArgType : ArgType<Path>(true) {
    override val description: kotlin.String
        get() = "{ Path }"

    override fun convert(value: kotlin.String, name: kotlin.String): Path = Paths.get(value)
}

fun build(ktsPath: Path): RootPath {
    val kts = ScriptEngineManager().getEngineByName("kotlin") ?: error("Kotlin script engine not found")
    val structure = kts.eval(ktsPath.readText())
    if (structure !is RootPath) {
        error("Script must define a root path")
    }
    return structure
}

inline fun <T, reified E : Exception> attempt(action: () -> T, catchAction: (e: E) -> Unit): T? {
    try {
        return action()
    } catch (e: Exception) {
        if (e !is E) {
            throw e
        }
        catchAction(e)
        return null
    }
}

@OptIn(ExperimentalCli::class, ExperimentalPathApi::class)
fun main(args: Array<String>) {
    val parser = ArgParser("example")
    val path by parser.option(
        ExistingPathArgType, "path", description = "Path to the coho script file", shortName = "i"
    ).default(Paths.get("coho.kts"))
    val buildPath by parser.option(
        PathArgType, "build-path", description = "Path to the build directory", shortName = "B"
    ).default(Paths.get("build"))
    val useServer by parser.option(
        ArgType.Boolean, "serve", description = "Show a live-updating localhost webserver", shortName = "s"
    ).default(false)
    parser.parse(args)

    var structure = build(path)
    structure.forceGenerate(buildPath)
    if (useServer) {
        runLocalServer(buildPath)
        structure.watch(ignorePaths = setOf(buildPath)) {
            try {
                structure = build(path)
            } catch (e: ScriptException) {
                println("Failed to run script: ${e.message}")
                return@watch
            }

            val tempDir =
                attempt<_, Exception>({ structure.forceGenerate(createTempDirectory(buildPath.name)) }) {
                    println("Failed to generate files: ${it.message} (${it::class.simpleName})")
                    return@watch
                }
            if (tempDir == null) {
                return@watch
            }
            buildPath.deleteRecursively()
            tempDir.copyToRecursively(buildPath, followLinks = true, overwrite = true)
            tempDir.deleteRecursively()
        }
    }
}