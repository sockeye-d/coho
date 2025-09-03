package dev.fishies.coho.core

import java.nio.file.Path
import kotlin.io.path.name
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue

abstract class Element(val name: String) {
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
                info("[${(doneCount * 100 / maxCount).coerceIn(0..100).toString().padStart(3, ' ')}%] ${it.name}")
            }
        }
        executionTime = time
        return location
    }

    open val count: Int = 1

    private fun Duration?.color() = when (this) {
        null -> ""
        in Duration.ZERO..<10.milliseconds -> fg(Color.Green)
        in 10.milliseconds..<100.milliseconds -> fg(Color.YELLOW)
        in 100.milliseconds..Duration.INFINITE -> fg(Color.RED)
        else -> fg(Color.Cyan)
    }
    protected val prefix
        get() = if (executionTime != null) "${executionTime.color()}(${executionTime})${RESET} " else ""

    override fun toString() = "$prefix$name (${this::class.simpleName})"

    companion object {
        @JvmStatic
        protected var doneCount = 0

        @JvmStatic
        protected var maxCount = 0
        var showProgress = false
    }
}