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
import java.util.concurrent.TimeUnit
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.thread
import kotlin.io.path.*
import kotlin.jvm.java
import kotlin.time.TimeSource

private object PathArgType : ArgType<Path>(true) {
    override val description: kotlin.String
        get() = "{ Path }"

    override fun convert(value: kotlin.String, name: kotlin.String): Path = Paths.get(value)
}

private fun build(ktsPath: Path): RootPath? {
    val kts = ScriptEngineManager().getEngineByName("kotlin") ?: error("Kotlin script engine not found")
    val structure: Any? = try {
        if (ktsPath.extension == "kt") {
            kts.eval(ktsPath.readText() + "\ngenerate()")
        } else {
            kts.eval(ktsPath.reader())
        }
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

private inline fun <T, reified E : Exception> attempt(action: () -> T, catchAction: (e: E) -> Unit): T? {
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

private val r by lazy { object {}::class.java }

private fun copyTemplateFile(resource: String, dest: Path, force: Boolean): Unit? {
    if (dest.exists()) {
        if (force) {
            note("Force overwriting ${dest.absolute()} with $resource")
        } else {
            err("Cannot copy template file $resource to $dest")
            info("force with --force")
            return null
        }
    }
    dest.writeBytes(r.getResource(resource)!!.readBytes())
    pos("Created $dest")
    return Unit
}

private fun createProject(scriptPath: Path, force: Boolean): Unit? {
    copyTemplateFile("/template/coho.kts", scriptPath, force) ?: return null
    copyTemplateFile("/template/index.md", Paths.get("index.md"), force) ?: return null
    return Unit
}

@OptIn(ExperimentalCli::class, ExperimentalPathApi::class, ExperimentalAtomicApi::class)
fun main(args: Array<String>) {
    val parser = ArgParser("coho")
    val cohoScriptPath by parser.option(
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
        ArgType.Boolean, "force", "f", "Force overwrite existing files to create a new coho project"
    ).default(false)
    val create by parser.option(ArgType.Boolean, "create", description = "Create a new coho script file").default(false)
    val noProgress by parser.option(ArgType.Boolean, "no-progress", description = "Don't show progress").default(false)
    parser.parse(args)
    ANSI.showVerbose = verbose
    Element.showProgress = !noProgress

    if (create) {
        createProject(cohoScriptPath, force) ?: return
    } else if (!cohoScriptPath.exists()) {
        err("coho script $cohoScriptPath does not exist")
        note("create a new coho project with --create")
        return
    }

    RootPath.rootBuildPath = buildPath

    note("Evaluating $cohoScriptPath...")
    val evalTimer = TimeSource.Monotonic.markNow()
    var structure = build(cohoScriptPath)
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

    val watchExit = AtomicBoolean(false)
    val hookExit = AtomicBoolean(true)
    val stopServerHook = thread(start = false, name = "StopServerHook") {
        watchExit.store(true)
        while (hookExit.load()) {
            Thread.sleep(50)
        }
    }
    Runtime.getRuntime().addShutdownHook(stopServerHook)

    structure.watch(ignorePaths = setOf(buildPath), exit = watchExit) {
        try {
            structure = build(cohoScriptPath)
        } catch (e: ScriptException) {
            err("Failed to run script: ${e.message}")
            return@watch false
        }

        if (structure == null) {
            return@watch false
        }

        val tempBuildPath = createTempDirectory("coho-build-${buildPath.name}")
        RootPath.rootBuildPath = buildPath
        val tempDir = attempt({ structure!!.forceGenerate(tempBuildPath) }) { ex: Exception ->
            err("Failed to generate file ${ex.message} (${ex::class.simpleName})")
            tempBuildPath.deleteRecursively()
            return@watch false
        }
        if (tempDir == null) {
            tempBuildPath.deleteRecursively()
            return@watch false
        }
        buildPath.deleteRecursively()
        tempDir.copyToRecursively(buildPath, followLinks = true, overwrite = true)
        tempDir.deleteRecursively()
        reload.value++
        true
    }
    info("Stopping server")
    try {
        server.stop(500, 500, TimeUnit.MILLISECONDS)
    } catch (_: Exception) {
        err("Server failed to stopped nicely")
        return
    }
    pos("Server stopped")
    hookExit.store(false)
    Runtime.getRuntime().halt(0)
}
