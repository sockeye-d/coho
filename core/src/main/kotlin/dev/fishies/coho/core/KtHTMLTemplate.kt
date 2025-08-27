package dev.fishies.coho.core

import java.nio.file.Path
import java.nio.file.Paths
import javax.script.ScriptContext
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings
import kotlin.collections.iterator
import kotlin.io.path.readText

private fun runScript(kts: String, context: Map<String, Any>): String {
    val scriptEngine = ScriptEngineManager().getEngineByName("kotlin")!!
    val bindings = SimpleBindings()
    for ((key, value) in context) {
        bindings[key] = value
    }
    scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE)
    return scriptEngine.eval(kts).toString()
}

fun ktHtmlTemplate(
    source: Path,
    contentKey: String = "content",
    context: Map<String, Any> = emptyMap(),
): ProcessedMarkdownFile.(html: String) -> String {
    val template = source.readText()
    return { innerHtml ->
        templateHtml(template, { err("No closing tag found in file $path") }) {
            if (it.trim() == contentKey) {
                // optimization for basic templates
                // if the template is just <?kt content ?>,
                // then nothing needs to get executed
                innerHtml
            } else runScript(it, context + mapOf(contentKey to innerHtml))
        }
    }
}

fun ktHtmlTemplate(
    source: String,
    contentString: String = "content",
    context: Map<String, Any> = emptyMap(),
) = ktHtmlTemplate(Paths.get(source), contentString, context)