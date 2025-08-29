package dev.fishies.coho.core.html

import dev.fishies.coho.core.Element
import dev.fishies.coho.core.OutputPath
import dev.fishies.coho.core.err
import dev.fishies.coho.core.info
import java.nio.file.Path
import javax.script.ScriptContext
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings
import kotlin.collections.iterator
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

private fun String.substr(startIndex: Int, endIndex: Int) =
    if (endIndex < 0) substring(startIndex) else substring(startIndex, endIndex)

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

class KtHTMLFile(val path: Path, val context: Map<String, Any>) : Element(path.name) {

    override fun _generate(location: Path): List<Path> = listOf(location.resolve(name).apply {
        writeText(templateHtml(path.readText(), { err("No closing tag found in file $path") }) {
            runScript(it)
        })
    })

    private fun runScript(cleanText: String): String {
        val fullContext = context + globalContext
        if (cleanText.trim() in fullContext) {
            info("Simple replacement detected on $cleanText, not evaluating", verbose = true)
            return fullContext[cleanText.trim()].toString()
        }
        val scriptEngine = engineManager.getEngineByName("kotlin")!!
        val bindings = SimpleBindings()
        for ((key, value) in fullContext) {
            bindings[key] = value
        }
        scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE)
        return scriptEngine.eval(cleanText).toString()
    }

    override fun toString() = "$name (${this::class.simpleName} $path)"

    companion object {
        private val engineManager = ScriptEngineManager()
        var globalContext: Map<String, Any> = emptyMap()
    }
}

fun OutputPath.ktHtml(source: Path, context: Map<String, Any> = emptyMap()) = children.add(KtHTMLFile(source, context))