package dev.fishies.coho.core

import java.nio.file.Path
import kotlin.io.path.name

abstract class Element(val name: String) {
    protected abstract fun _generate(location: Path): List<Path>

    fun generate(location: Path): Path {
        _generate(location).forEach {
            doneCount++
            if (doneCount > maxCount) {
                maxCount = doneCount
            }
            info("generated $it", verbose = true)
            if (showProgress) {
                // print("\r$ERASE_LINE[${(doneCount * 100 / maxCount).coerceIn(0..100).toString().padStart(3, ' ')}%] ${it.name}")
                info("[${(doneCount * 100 / maxCount).coerceIn(0..100).toString().padStart(3, ' ')}%] ${it.name}")
            }
        }
        return location
    }

    open val count: Int = 1

    override fun toString() = "$name (${this::class.simpleName})"

    companion object {
        @JvmStatic
        protected var doneCount = 0
        @JvmStatic
        protected var maxCount = 0
        var showProgress = false
    }
}