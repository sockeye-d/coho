package dev.fishies.coho.core.html

import dev.fishies.coho.core.*
import dev.fishies.coho.core.html.KtHTMLFile.Companion.globalContext
import dev.fishies.coho.core.scripting.eval
import java.nio.file.Path
import kotlin.io.path.*

private fun String.substr(startIndex: Int, endIndex: Int) =
    if (endIndex < 0) substring(startIndex) else substring(startIndex, endIndex)

fun runScript(kts: String, context: Map<String, Any?>, name: String? = null): String {
    val fullContext = context + globalContext
    if (kts.trim() in fullContext) {
        info("Simple replacement detected on $kts, not evaluating", verbose = true)
        return fullContext[kts.trim()].toString()
    }
    return eval(kts, fullContext, name).toString()
}

fun templateHtml(html: String, onErrorAction: () -> Unit, templateAction: (String) -> String): String {
    val builder = StringBuilder()
    var lastOpenIndex = 0
    var openIndex = 0
    do {
        lastOpenIndex = openIndex
        openIndex = html.indexOf("<?kt", openIndex + 1)
        builder.append(html.substr(lastOpenIndex, openIndex))
        if (openIndex == -1) {
            continue
        }

        val closeIndex = html.indexOf("?>", openIndex)

        if (closeIndex < 0) {
            onErrorAction()
            break
        }

        val cleanText = html.substr(openIndex + 4, closeIndex)
        openIndex = closeIndex + 2

        builder.append(templateAction(cleanText))
    } while (openIndex > 0)
    return builder.toString()
}

class KtHTMLFile(val path: Path, val context: Map<String, Any?>) : Element(path.name) {

    override fun _generate(location: Path): List<Path> = listOf(location.resolve(name).apply {
        writeText(templateHtml(path.readText(), { err("No closing tag found in file $path") }) {
            runScript(it, context, path.toString())
        })
    })

    companion object {
        var globalContext: Map<String, Any?> = emptyMap()
    }
}

fun OutputPath.ktHtml(source: Path, context: Map<String, Any?> = emptyMap()) = children.add(KtHTMLFile(source, context))