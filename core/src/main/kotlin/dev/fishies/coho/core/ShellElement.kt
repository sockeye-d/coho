package dev.fishies.coho.core

import java.nio.file.Path
import kotlin.io.path.absolutePathString

class ShellElement(
    private val executable: String,
    private val workingDirectory: Path,
    private val arguments: List<Any>,
    private val showStdOut: Boolean
) : Element("[$workingDirectory] $executable ${arguments.joinToString(" ")}") {
    override fun _generate(location: Path): List<Path> {
        val proc =
            ProcessBuilder().directory(workingDirectory.toFile()).command(executable, *arguments.map {
                when (it) {
                    is Path -> location.resolve(it).absolutePathString()
                    else -> it.toString()
                }
            }.toTypedArray())
                .redirectOutput(if (showProgress && showStdOut) ProcessBuilder.Redirect.INHERIT else ProcessBuilder.Redirect.DISCARD)
                .start()!!
        val err = proc.waitFor()
        if (err != 0) {
            err("$executable $arguments exited with code $err")
        }
        return emptyList()
    }

    override val count = 0
}

fun OutputPath.shell(
    executable: String, vararg arguments: Any, workingDirectory: Path = buildPath, showStdOut: Boolean = true
) = children.add(ShellElement(executable, workingDirectory, arguments.toList(), showStdOut))