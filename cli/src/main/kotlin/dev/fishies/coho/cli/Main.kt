package dev.fishies.coho.cli

import dev.fishies.coho.core.ANSI
import dev.fishies.coho.core.RootPath
import dev.fishies.coho.core.err
import dev.fishies.coho.core.info
import dev.fishies.coho.core.pos
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.default
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.BindException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import kotlin.io.path.*

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

fun build(ktsPath: Path): RootPath? {
    val kts = ScriptEngineManager().getEngineByName("kotlin") ?: error("Kotlin script engine not found")
    val structure = kts.eval(ktsPath.readText())
    if (structure !is RootPath) {
        err("Script must define a root path")
        return null
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
    val serverPort by parser.option(ArgType.Int, "server-port", description = "Integrated server port", shortName = "p")
        .default(8080)
    val noReloadScript by parser.option(
        ArgType.Boolean,
        "no-reload-script",
        description = "Don't inject the hot reload script into HTML files (only when using integrated server, it's never included in build outputs)"
    ).default(false)
    parser.parse(args)

    ANSI.noColor = System.getenv("NO_COLOR")?.isNotEmpty() ?: false

    var structure = build(path)
    if (structure == null) {
        return
    }
    structure.forceGenerate(buildPath)
    if (!useServer) {
        return
    }

    val reload = MutableStateFlow(0)
    val server = attempt({ runLocalServer(buildPath, reload, noReloadScript, serverPort) }) { e: BindException ->
        err("Port $serverPort already in use")
        return
    }!!

    pos("Running local server on http://127.0.0.1:$serverPort")

    Runtime.getRuntime().addShutdownHook(Thread {
        info("\nStopping server")
        server.stop()
        pos("Server stopped")
    })

    structure.watch(ignorePaths = setOf(buildPath)) {
        try {
            structure = build(path)
        } catch (e: ScriptException) {
            err("Failed to run script: ${e.message}")
            return@watch false
        }

        if (structure == null) {
            return@watch false
        }

        val tempDir = attempt<_, Exception>({ structure!!.forceGenerate(createTempDirectory(buildPath.name)) }) {
            err("Failed to generate file ${it.message} (${it::class.simpleName})")
            return@watch false
        }
        if (tempDir == null) {
            return@watch false
        }
        buildPath.deleteRecursively()
        tempDir.copyToRecursively(buildPath, followLinks = true, overwrite = true)
        tempDir.deleteRecursively()
        reload.value++
        true
    }
}