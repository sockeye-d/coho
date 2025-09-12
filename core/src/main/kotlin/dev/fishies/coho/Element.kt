package dev.fishies.coho

import dev.fishies.coho.core.TerminalColor
import dev.fishies.coho.core.reset
import dev.fishies.coho.core.fg
import dev.fishies.coho.core.info
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue

/**
 * The base output element type, representing a single file in the output directory.
 * Subclasses must implement [_generate] to specify how that file should be generated.
 */
abstract class Element(val name: String) {
    /**
     * @return A list of the files generated, for progress reports.
     */
    protected abstract fun _generate(location: Path): List<Path>
    var executionTime: Duration? = null

    open fun generate(location: Path? = null): Path {
        requireNotNull(location)
        val (paths, time) = measureTimedValue { _generate(location) }
        paths.forEach {
            doneCount++
            if (doneCount > maxCount) {
                maxCount = doneCount
            }
            info("generated $it", verbose = true)
            if (showProgress) {
                val percentage = "[${(doneCount * 100 / maxCount).coerceIn(0..100).toString().padStart(3, ' ')}%]"
                info("$percentage ${RootPath.rootBuildPath.relativize(it)}")
            }
        }
        executionTime = time
        return location
    }

    open val count: Int = 1

    private fun Duration?.color() = when (this) {
        null -> ""
        in Duration.ZERO..<10.milliseconds -> fg(TerminalColor.Green)
        in 10.milliseconds..<100.milliseconds -> fg(TerminalColor.Yellow)
        in 100.milliseconds..Duration.INFINITE -> fg(TerminalColor.Red)
        else -> fg(TerminalColor.Cyan)
    }

    protected val prefix
        get() = if (executionTime != null) "${executionTime.color()}(${executionTime})${reset} " else ""

    override fun toString() = "$prefix$name (${this::class.simpleName})"

    companion object {
        @JvmStatic
        protected var doneCount = 0

        @JvmStatic
        protected var maxCount = 0
        var showProgress = false
    }
}