package dev.fishies.coho.html

import dev.fishies.coho.Element
import dev.fishies.coho.OutputPath
import dev.fishies.coho.core.err
import dev.fishies.coho.runScript
import dev.fishies.coho.templateString
import java.nio.file.Path
import kotlin.io.path.*

/**
 * @suppress
 */
class KtHTMLFile(val path: Path, val context: Map<String, Any?>) : Element(path.name) {

    override fun _generate(location: Path): List<Path> = listOf(location.resolve(name).apply {
        writeText(templateString(path.readText(), { err("No closing tag found in file $path") }) {
            runScript(it, context, path.toString())
        })
    })

    companion object {
        var globalContext: Map<String, Any?> = emptyMap()
    }
}

/**
 * Template the [source] file using the `<?kt ... ?>` syntax.
 * For example,
 * ```
 * <p><?kt (1..5).joinToString("\n") { "<a href='hi$it'>hi#$it</a>" } ?></p>
 * ```
 * results in
 * ```
 * <p><a href='hi1'>hi#1</a>
 * <a href='hi2'>hi#2</a>
 * <a href='hi3'>hi#3</a>
 * <a href='hi4'>hi#4</a>
 * <a href='hi5'>hi#5</a></p>
 * ```
 * You can pass global variables into the templates with [context].
 */
fun OutputPath.ktHtml(source: Path, context: Map<String, Any?> = emptyMap()) = children.add(KtHTMLFile(source, context))