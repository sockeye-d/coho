package dev.fishies.coho

import dev.fishies.coho.core.PrismBundleGrammarLocator
import dev.fishies.coho.core.Prism_qml
import dev.fishies.coho.core.err
import dev.fishies.coho.core.info
import dev.fishies.coho.core.scripting.eval
import dev.fishies.coho.html.KtHTMLFile
import io.noties.prism4j.AbsVisitor
import io.noties.prism4j.GrammarLocator
import io.noties.prism4j.Prism4j
import org.apache.commons.text.StringEscapeUtils
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.pathString
import kotlin.time.measureTime

/**
 * Run an [executable] with [arguments] in [workingDirectory] and capture its stdout.
 * This runs at evaluation time, not build time.
 */
fun OutputPath.exec(executable: String, vararg arguments: Any, workingDirectory: Path? = null): String {
    val proc = ProcessBuilder().directory((workingDirectory ?: source.sourcePath).toFile())
        .command(executable, *arguments.map {
            when (it) {
                is Path -> source.sourcePath.resolve(it).absolute().normalize().pathString
                else -> it.toString()
            }
        }.toTypedArray()).redirectOutput(ProcessBuilder.Redirect.PIPE).redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()!!
    val err = proc.waitFor()
    if (err != 0) {
        err("$executable ${arguments.toList()} exited with code $err")
    }
    return proc.inputReader().readText()
}

/**
 * Run an [executable] with [arguments] in [workingDirectory] while providing [stdin] and capture its stdout.
 * This runs at evaluation time, not build time.
 */
fun exec(executable: String, vararg arguments: String, workingDirectory: Path? = null, stdin: String? = null): String {
    val proc = ProcessBuilder().directory(workingDirectory?.toFile()).command(executable, *arguments)
        .redirectOutput(ProcessBuilder.Redirect.PIPE).redirectError(ProcessBuilder.Redirect.INHERIT).start()!!
    if (stdin != null) {
        proc.outputWriter().write(stdin)
        proc.outputWriter().close()
    }
    val err = proc.waitFor()
    if (err != 0) {
        err("$executable ${arguments.toList()} exited with code $err")
    }
    return proc.inputReader().readText()
}

/**
 * Escape this to an HTML-compatible string literal.
 * Internally this uses [org.apache.commons.text.StringEscapeUtils.escapeHtml4]
 * @receiver The string to escape to an HTML string.
 */
fun String.escapeHtml(): String = StringEscapeUtils.escapeHtml4(this)

/**
 * Escape this to an XML-compatible string literal.
 * Internally this uses [org.apache.commons.text.StringEscapeUtils.escapeXml11]
 * @receiver The string to escape to an XML string.
 */
fun String.escapeXml(): String = StringEscapeUtils.escapeXml11(this)

/**
 * Unescape this HTML-compatible literal to a plain string.
 * Internally this uses [org.apache.commons.text.StringEscapeUtils.unescapeHtml4]
 * @receiver The HTML string to unescape to a plain string.
 */
fun String.unescapeHtml(): String = StringEscapeUtils.unescapeHtml4(this)

/**
 * Unescape this XML-compatible literal to a plain string.
 * Internally this uses [org.apache.commons.text.StringEscapeUtils.unescapeXml]
 * @receiver The XML string to unescape to a plain string.
 */
fun String.unescapeXml(): String = StringEscapeUtils.unescapeXml(this)

/**
 * Highlight [this] according to [language].
 * If the language doesn't exist, it returns null.
 */
fun String.highlight(language: String): String? {
    val sb = StringBuilder()
    val time = measureTime {
        val prism = Prism4j(object : GrammarLocator {
            val delegate = PrismBundleGrammarLocator()
            override fun grammar(prism4j: Prism4j, language: String) = when (language) {
                "qml" -> Prism_qml.create(prism4j)
                else -> delegate.grammar(prism4j, language)
            }

            override fun languages() = delegate.languages() + "qml"
        })
        val grammar = language.run { prism.grammar(this) } ?: return null
        val tokens = prism.tokenize(this, grammar)
        val tokenVisitor: AbsVisitor = object : AbsVisitor() {
            override fun visitText(text: Prism4j.Text) {
                sb.append("<span class=\"code-text code-$language-text\">${text.literal().escapeHtml()}</span>")
            }

            override fun visitSyntax(syntax: Prism4j.Syntax) {
                val firstChild = syntax.children().first()
                if (syntax.children().size == 1 && firstChild is Prism4j.Text) {
                    val inner = firstChild.literal().escapeHtml()
                    val classes = mutableListOf<String>()
                    classes.add("code-${syntax.type()}")
                    syntax.alias()?.apply { classes.add("code-$this") }
                    classes.add("code-$language-${syntax.type()}")
                    syntax.alias()?.apply { classes.add("code-$language-$this") }
                    sb.append("<span class=\"${classes.joinToString(" ")}\">$inner</span>")
                } else {
                    visit(syntax.children())
                }
            }
        }
        tokenVisitor.visit(tokens)
    }
    info("Highlighting $language (length: $length) took $time", verbose = true)
    return sb.toString()
}

private fun String.substr(startIndex: Int, endIndex: Int) =
    if (endIndex < 0) substring(startIndex) else substring(startIndex, endIndex)

/**
 * Run the given Kotlin script [kts] with [context].
 *
 * @param kts The script to run
 * @param context The context (global variables) to give to that script.
 * @param name The name of the script to show in diagnostic messages.
 */
fun runScript(kts: String, context: Map<String, Any?>, name: String? = null): String {
    val fullContext = context + KtHTMLFile.Companion.globalContext
    if (kts.trim() in fullContext) {
        info("Simple replacement detected on $kts, not evaluating", verbose = true)
        return fullContext[kts.trim()].toString()
    }
    return eval(kts, fullContext, name).toString()
}

/**
 * Template the given [string] string using [templateAction].
 *
 * @param string The string to template. It doesn't necessarily have to be HTML
 * @param onErrorAction Runs when the Kotlin templated area is unclosed.
 * @param templateAction Given what the template contains, returns what it should be replaced with.
 */
fun templateString(string: String, onErrorAction: () -> Unit, templateAction: (String) -> String): String {
    val builder = StringBuilder()
    var lastOpenIndex = 0
    var openIndex = 0
    do {
        lastOpenIndex = openIndex
        openIndex = string.indexOf("<?kt", openIndex + 1)
        builder.append(string.substr(lastOpenIndex, openIndex))
        if (openIndex == -1) {
            continue
        }

        val closeIndex = string.indexOf("?>", openIndex)

        if (closeIndex < 0) {
            onErrorAction()
            break
        }

        val cleanText = string.substr(openIndex + 4, closeIndex)
        openIndex = closeIndex + 2

        builder.append(templateAction(cleanText))
    } while (openIndex > 0)
    return builder.toString()
}