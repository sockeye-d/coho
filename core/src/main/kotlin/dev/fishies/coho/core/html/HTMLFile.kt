package dev.fishies.coho.core.html

import dev.fishies.coho.core.CopyFile
import dev.fishies.coho.core.OutputPath
import java.nio.file.Path

class HTMLFile(path: Path) : CopyFile(path)

fun OutputPath.html(source: Path) = children.add(HTMLFile(source))