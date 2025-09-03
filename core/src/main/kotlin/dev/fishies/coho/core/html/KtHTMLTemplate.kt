package dev.fishies.coho.core.html

import dev.fishies.coho.core.err
import java.nio.file.Path
import kotlin.io.path.readText

fun ktHtmlTemplate(
    source: Path,
    contentKey: String = "content",
    context: Map<String, Any> = emptyMap(),
): (html: String) -> String = { innerHtml ->
    templateHtml(source.readText(), { err("No closing tag found in file $source") }) {
        runScript(it, context + mapOf(contentKey to innerHtml), source.toString())
    }
}

fun ktTemplate(source: Path, context: Map<String, Any> = emptyMap()) =
    templateHtml(source.readText(), { err("No closing tag found in file $source") }) {
        runScript(it, context, source.toString())
    }