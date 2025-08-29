package dev.fishies.coho.cli

import dev.fishies.coho.core.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.default
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.BindException
import java.nio.file.Path
import java.nio.file.Paths
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import kotlin.concurrent.thread
import kotlin.io.path.*
import kotlin.time.TimeSource

object PathArgType : ArgType<Path>(true) {
    override val description: kotlin.String
        get() = "{ Path }"

    override fun convert(value: kotlin.String, name: kotlin.String): Path = Paths.get(value)
}

fun build(ktsPath: Path): RootPath? {
    val kts = ScriptEngineManager().getEngineByName("kotlin") ?: error("Kotlin script engine not found")
    var structure: Any?
    try {
        structure = kts.eval(ktsPath.readText())
    } catch (e: ScriptException) {
        err("Failed to run script: ${e.message}")
        return null
    }
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
    val parser = ArgParser("coho")
    val path by parser.option(
        PathArgType, "path", description = "Path to the coho script file", shortName = "i"
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
    val verbose by parser.option(ArgType.Boolean, "verbose", "v", "Show extra information").default(false)
    val force by parser.option(
        ArgType.Boolean, "force", "f", "Force overwrite existing files to create a new coho script"
    ).default(false)
    val create by parser.option(ArgType.Boolean, "create", description = "Create a new coho script file").default(false)
    val noProgress by parser.option(ArgType.Boolean, "no-progress", description = "Don't show progress").default(false)
    parser.parse(args)
    ANSI.showVerbose = verbose
    Element.showProgress = !noProgress

    if (create) {
        if (path.exists()) {
            if (force) {
                note("force overwriting $path")
            } else {
                err("$path already exists")
                note("force creation of $path with --force")
                return
            }
        }

        path.writeText(
            """
            import dev.fishies.coho.core.*
            // plain html and html templating functions
            import dev.fishies.coho.core.html.*
            // markdown functions like `md` and `basicMd`
            import dev.fishies.coho.core.markdown.*
            import dev.fishies.coho.core.shell.*
            import java.nio.file.Path
            import kotlin.io.path.*

            root {
                // your code goes here
            }
        """.trimIndent()
        )
        pos("Created $path")
    } else if (!path.exists()) {
        err("coho script $path does not exist")
        note("create a new coho script file with --create")
        return
    }

    RootPath.rootBuildPath = buildPath

    note("Evaluating $path...")
    val evalTimer = TimeSource.Monotonic.markNow()
    var structure = build(path)
    @Suppress("FoldInitializerAndIfToElvis", "RedundantSuppression") if (structure == null) {
        return
    }
    pos("Evaluation complete in ${evalTimer.elapsedNow()}")

    note("Building...")
    val rebuildTimer = TimeSource.Monotonic.markNow()
    structure.forceGenerate(buildPath)
    pos("Rebuild complete in ${rebuildTimer.elapsedNow()}")
    if (!useServer) {
        return
    }

    note("Starting server...")

    val reload = MutableStateFlow(0)
    val server = attempt({ runLocalServer(buildPath, reload, noReloadScript, serverPort) }) { _: BindException ->
        err("Port $serverPort already in use")
        return
    }!!

    pos("Running local server on http://localhost:$serverPort")
    if (!noReloadScript) {
        note("Hot reload is active")
    }

    val currentClassLoader = Thread.currentThread().contextClassLoader
    Runtime.getRuntime().addShutdownHook(
        thread(
            start = false, contextClassLoader = Thread.currentThread().contextClassLoader, name = "StopServerHook"
        ) {
            Thread.currentThread().contextClassLoader = currentClassLoader
            info("Stopping server")
            try {
                server.stop()
            } catch (_: Exception) {
                err("Server failed to stopped nicely")
                return@thread
            }
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

        val tempBuildPath = createTempDirectory(buildPath.name)
        RootPath.rootBuildPath = buildPath
        val tempDir = attempt({ structure!!.forceGenerate(tempBuildPath) }) { ex: Exception ->
            err("Failed to generate file ${ex.message} (${ex::class.simpleName})")
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