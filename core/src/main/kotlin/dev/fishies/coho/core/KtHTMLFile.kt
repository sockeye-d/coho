package dev.fishies.coho.core

import java.nio.file.Path
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings
import kotlin.io.path.copyTo
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.text.indexOf

class KtHTMLFile(val path: Path, val context: Map<String, Any>) : Element(path.name) {
    private fun String.substr(startIndex: Int, endIndex: Int) =
        if (endIndex < 0) substring(startIndex) else substring(startIndex, endIndex)

    override fun _generate(location: Path) {
        val text = path.readText()
        val builder = StringBuilder()
        var lastOpenIndex = 0
        var openIndex = 0
        do {
            lastOpenIndex = openIndex
            openIndex = text.indexOf("<?kt", openIndex + 1)
            builder.append(text.substr(lastOpenIndex, openIndex))
            if (openIndex == -1) {
                continue
            }

            val closeIndex = text.indexOf("?>", openIndex)
            assert(closeIndex >= 0) { "No closing tag found in file $path" }
            val cleanText = text.substr(openIndex + 4, closeIndex)
            openIndex = closeIndex + 2

            val scriptEngine = engineManager.getEngineByName("kotlin")!!
            val bindings = SimpleBindings()
            for ((key, value) in context) {
                bindings[key] = value
            }
            scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE)
            builder.append(scriptEngine.eval(cleanText).toString())
        } while (openIndex > 0)

        location.resolve(name).writeText(builder)
    }

    override fun toString() = "$name (${this::class.simpleName} $path)"

    companion object {
        private val engineManager = ScriptEngineManager()
    }
}

fun OutputPath.ktHtml(source: Path, context: Map<String, Any> = emptyMap()) = children.add(KtHTMLFile(source, context))