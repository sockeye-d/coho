package dev.fishies.coho.core

import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.pathString
import kotlin.time.Duration
import kotlin.time.TimeSource

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
