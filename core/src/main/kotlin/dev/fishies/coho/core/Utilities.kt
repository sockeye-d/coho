package dev.fishies.coho.core

import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.pathString

/**
 * Run an [executable] with [arguments] in [workingDirectory] and capture its stdout.
 * This runs at evaluation time, not build time.
 */
fun OutputPath.exec(executable: String, vararg arguments: Any, workingDirectory: Path? = null): String {
    val proc = ProcessBuilder().directory((workingDirectory ?: source.sourcePath).toFile())
        .command(executable, *arguments.map {
            when (it) {
                is Path -> source.sourcePath.resolve(it).absolute().normalize().pathString
                else -> it.toString()
            }
        }.toTypedArray()).redirectOutput(ProcessBuilder.Redirect.PIPE).redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()!!
    val err = proc.waitFor()
    if (err != 0) {
        err("$executable ${arguments.toList()} exited with code $err")
    }
    return proc.inputReader().readText()
}

/**
 * Run an [executable] with [arguments] in [workingDirectory] while providing [stdin] and capture its stdout.
 * This runs at evaluation time, not build time.
 */
fun exec(executable: String, vararg arguments: String, workingDirectory: Path? = null, stdin: String? = null): String {
    val proc = ProcessBuilder().directory(workingDirectory?.toFile()).command(executable, *arguments)
        .redirectOutput(ProcessBuilder.Redirect.PIPE).redirectError(ProcessBuilder.Redirect.INHERIT).start()!!
    if (stdin != null) {
        proc.outputWriter().write(stdin)
        proc.outputWriter().close()
    }
    val err = proc.waitFor()
    if (err != 0) {
        err("$executable ${arguments.toList()} exited with code $err")
    }
    return proc.inputReader().readText()
}
