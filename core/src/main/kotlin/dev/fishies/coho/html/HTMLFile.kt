package dev.fishies.coho.html

import dev.fishies.coho.CopyFile
import dev.fishies.coho.OutputPath
import java.nio.file.Path

/**
 * @suppress
 */
class HTMLFile(path: Path) : CopyFile(path)

/**
 * Copy an untemplated HTML file from the source to the destination.
 */

fun OutputPath.html(source: Path) = children.add(HTMLFile(source))