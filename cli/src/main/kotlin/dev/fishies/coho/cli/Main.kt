package dev.fishies.coho.cli

import dev.fishies.coho.Element
import dev.fishies.coho.RootPath
import dev.fishies.coho.core.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.cli.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.BindException
import java.nio.file.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.thread
import kotlin.io.path.*
import kotlin.system.exitProcess
import kotlin.time.TimeSource
import kotlin.time.measureTime

@Suppress("unused")
enum class Shell(val path: String) {
    Nu("coho.nu"), Zsh("_coho"),
}

@OptIn(ExperimentalCli::class, ExperimentalPathApi::class, ExperimentalAtomicApi::class)
fun main(args: Array<String>) {
    val parser = ArgParser("coho")
    val cohoScriptPath by parser.argument(
        PathArgType, fullName = "path", description = "Path to the coho script file"
    ).optional().default(Paths.get("main.coho.kts"))
    val buildPath by parser.option(
        PathArgType, "build-path", description = "Path to the build directory", shortName = "B"
    ).default(Paths.get("build"))
    val verbose by parser.option(ArgType.Boolean, "verbose", "v", "Show extra information").default(false)
    val force by parser.option(
        ArgType.Boolean, "force", "f", "Force overwrite existing files to create a new coho project"
    ).default(false)
    val create by parser.option(ArgType.Boolean, "create", description = "Create a new coho script file").default(false)
    val noProgress by parser.option(ArgType.Boolean, "no-progress", description = "Don't show progress").default(false)
    val debugTimes by parser.option(ArgType.Boolean, "show-execution-times", description = "Show execution times")
        .default(false)
    var useServer = false
    val serve = object : Subcommand("serve", "Show a live-updating localhost webserver") {
        val serverPort by option(
            ArgType.Int, "server-port", description = "Integrated server port", shortName = "p"
        ).default(8080)
        val portRetryCount by option(
            ArgType.Int,
            "port-retry-count",
            description = "How many times to try different ports if the current port is already in use (default: infinite)"
        )
        val noReloadScript by option(
            ArgType.Boolean,
            "no-reload-script",
            description = "Don't inject the hot reload script into HTML files (only when using integrated server, it's never included in build outputs)"
        ).default(false)

        override fun execute() {
            useServer = true
        }
    }
    val shell by parser.option(
        ArgType.Choice<Shell>(),
        "print-shell-completions",
        description = "Print shell completion scripts for various shells"
    )
    parser.subcommands(serve)
    parser.parse(args)
    if (shell != null) {
        println(resources.getResource("/shell/${shell!!.path}")?.readText())
        return
    }
    Ansi.showVerbose = verbose
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
    val structure = build(cohoScriptPath) ?: return
    pos("Evaluation complete in ${evalTimer.elapsedNow()}")

    note("Building...")
    pos("Build complete in ${measureTime { structure.generate(buildPath) }}")
    if (debugTimes) {
        info(structure.toString())
    }
    if (!useServer) {
        return
    }

    note("Starting server...")

    val reload = MutableStateFlow(0)
    val server = findServerPort(serve.portRetryCount, serve.serverPort, buildPath, reload, serve.noReloadScript)

    if (server == null) {
        err("All attempted ports were already in use")
        return
    }

    if (!serve.noReloadScript) {
        note("Hot reload is active")
    }

    val watchExit = AtomicBoolean(false)
    val stopServerHook = thread(start = false, name = "StopServerHook") {
        watchExit.store(true)
    }
    Runtime.getRuntime().addShutdownHook(stopServerHook)

    structure.watch(ignorePaths = setOf(buildPath), exit = watchExit) {
        var structure: RootPath? = null
        val tempBuildPath = createTempDirectory("coho-build-${buildPath.name}")
        RootPath.rootBuildPath = tempBuildPath
        pos(
            "Evaluation complete in ${
                measureTime {
                    structure = build(cohoScriptPath)
                }
            }")

        if (structure == null) {
            tempBuildPath.deleteRecursively()
            return@watch false
        }

        // """atomic operations"""
        try {
            structure.generate(tempBuildPath)
        } catch (ex: Exception) {
            err("Failed to generate file ${ex.message} (${ex::class.simpleName})")
            tempBuildPath.deleteRecursively()
            return@watch false
        }
        if (debugTimes) {
            info(structure.toString())
        }
        buildPath.deleteRecursively()
        tempBuildPath.copyToRecursively(buildPath, followLinks = true, overwrite = true)
        tempBuildPath.deleteRecursively()
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
    exitProcess(0)
}

private fun findServerPort(
    portRetryCount: Int?, serverPort: Int, buildPath: Path, reload: MutableStateFlow<Int>, noReloadScript: Boolean
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? {
    var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    for (port in if (portRetryCount != null) serverPort..serverPort + portRetryCount else serverPort..65535) {
        info("Connecting to port $port")
        try {
            server = runLocalServer(buildPath, reload, noReloadScript, port)
        } catch (_: BindException) {
            err("Port $serverPort already in use, retrying")
            server = null
            continue
        }
        pos("Running local server on http://localhost:$port")
        break
    }
    return server
}

private object PathArgType : ArgType<Path>(true) {
    override val description: kotlin.String
        get() = "{ Path }"

    override fun convert(value: kotlin.String, name: kotlin.String): Path = Paths.get(value)
}

private val resources by lazy { object {}::class.java }

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
    dest.writeBytes(resources.getResource(resource)!!.readBytes())
    pos("Created $dest")
    return Unit
}

private fun copyTemplateFile(resource: String, force: Boolean) =
    copyTemplateFile(resource, Paths.get(Paths.get(resource).name), force)

private fun createProject(scriptPath: Path, force: Boolean): Unit? {
    copyTemplateFile("/template/main.coho.kts", scriptPath, force) ?: return null
    copyTemplateFile("/template/markdown-template.html", force) ?: return null
    copyTemplateFile("/template/index.md", force) ?: return null
    copyTemplateFile("/template/style.css", force) ?: return null
    return Unit
}

@OptIn(ExperimentalAtomicApi::class)
fun RootPath.watch(ignorePaths: Set<Path>, exit: AtomicBoolean, rebuild: () -> Boolean) {
    val watcher = source.sourcePath.fileSystem.newWatchService()
    val keys = mutableMapOf<WatchKey, Path>()

    val watchDir: (Path) -> Unit = {
        keys[it.register(
            watcher,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )] = it
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

    watchLoop@ while (true) { // val key = watcher.getKey { return }
        var key: WatchKey? = null
        while (key == null) { // delay(50.milliseconds)
            Thread.sleep(50)
            if (exit.load()) break@watchLoop
            key = watcher.poll()
        }
        val dir = keys[key] ?: error("hi")

        for (event in key.pollEvents()) {
            val kind = event.kind()

            if (kind == StandardWatchEventKinds.OVERFLOW) {
                continue
            }

            val filename = event.context() as Path
            val fullPath = dir.resolve(filename)

            if (fullPath.notExists()) {
                continue
            }

            if (fullPath.isDirectory()) {
                continue
            }

            if (fullPath.isHidden()) {
                continue
            }

            if (fullPath.name.endsWith("~") || fullPath.name.startsWith(".")) {
                continue
            }

            if (fullPath.normalize() in ignorePaths) {
                continue
            }

            info(
                "$fullPath ${
                    when (kind) {
                        StandardWatchEventKinds.ENTRY_CREATE -> "created"
                        StandardWatchEventKinds.ENTRY_MODIFY -> "modified"
                        StandardWatchEventKinds.ENTRY_DELETE -> "delete"
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

            if (kind == StandardWatchEventKinds.ENTRY_CREATE && fullPath.isDirectory()) {
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