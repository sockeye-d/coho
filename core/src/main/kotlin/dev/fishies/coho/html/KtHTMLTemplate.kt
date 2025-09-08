package dev.fishies.coho.html

import dev.fishies.coho.core.err
import dev.fishies.coho.runScript
import dev.fishies.coho.templateString
import java.nio.file.Path
import kotlin.io.path.readText
import dev.fishies.coho.OutputPath

/**
 * Template the [source] like [ktHtml], but it returns a function you can then call with a string to be used as a
 * template.
 * For example, you could do
 * ```
 * <html>
 * <p>
 *     <? content ?>
 * </p>
 * </html>
 * ```
 * ```
 * val template = ktHtmlTemplate(src("template.html"))
 * ```
 * `content` will be replaced with the string you call `template` with:
 * ```
 * template("really good content")
 * ```
 * You can use this to implement a Markdown template in conjunction with [OutputPath.markdownTemplate]:
 * ```
 * root {
 *     // ...
 *     markdownTemplate = {
 *         val meta = frontmatter["meta"]?.asMap()
 *         val title: String? = meta?.get("title") as? String
 *         val description: String? = meta?.get("description") as? String
 *         val type: String? = meta?.get("type") as? String
 *
 *         ktHtmlTemplate(
 *             src("markdown-template.html"),
 *             context = mapOf("title" to title, "description" to description, "type" to type),
 *         )(it)
 *     }
 *     // ...
 * }
 * ```
 *
 * @see [ktHtml]
 */
fun ktMdTemplate(
    source: Path,
    contentKey: String = "content",
    context: Map<String, Any?> = emptyMap(),
): (html: String) -> String = { innerHtml ->
    templateString(source.readText(), { err("No closing tag found in file $source") }) {
        runScript(it, context + mapOf(contentKey to innerHtml), source.toString())
    }
}

/**
 * Immediately template the file at [source].
 *
 * @see [ktHtml]
 */
fun ktTemplate(source: Path, context: Map<String, Any?> = emptyMap()) =
    templateString(source.readText(), { err("No closing tag found in file $source") }) {
        runScript(it, context, source.toString())
    }