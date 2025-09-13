package dev.fishies.coho.core

import dev.fishies.coho.RootPath
import dev.fishies.coho.core.scripting.eval
import java.nio.file.Path

/**
 * @suppress
 */
fun build(ktsPath: Path): RootPath? {
    RootPath.scriptSourcePath = ktsPath
    val structure = eval(ktsPath)
    if (structure !is RootPath) {
        return null
    }
    return structure
}