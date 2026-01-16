package dev.fishies.coho

import dev.fishies.coho.core.*
import dev.fishies.coho.core.highlighting.Prism
import dev.fishies.coho.core.scripting.eval
import dev.fishies.coho.html.KtHtmlFile
import io.noties.prism4j.AbsVisitor
import io.noties.prism4j.Prism4j
import net.mamoe.yamlkt.Yaml
import org.apache.batik.dom.GenericDOMImplementation
import org.apache.batik.svggen.SVGGeneratorContext
import org.apache.batik.svggen.SVGGraphics2D
import org.apache.commons.text.StringEscapeUtils
import org.scilab.forge.jlatexmath.*
import org.w3c.dom.DOMImplementation
import org.w3c.dom.Document
import java.awt.Dimension
import java.io.StringWriter
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.time.measureTime

/**
 * Run an [executable] with [arguments] in [workingDirectory] and capture its stdout.
 * This runs at evaluation time, not build time.
 */
fun OutputPath.exec(executable: String, vararg arguments: Any, workingDirectory: Path? = null): String {
    val proc = ProcessBuilder().directory((workingDirectory ?: source.sourcePath).toFile())
        .command(executable, *arguments.map {
            when (it) {
                is Path -> it.absolute().normalize().pathString
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

private class HTMLVisitor(val sb: StringBuilder, val language: String) : AbsVisitor() {
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

private class ANSIVisitor(val sb: StringBuilder, val language: String) : AbsVisitor() {
    private val String.color: String
        get() = when (this) {
            "keyword" -> fg(TerminalColor.Magenta)
            "function" -> italic + fg(TerminalColor.Blue)
            "number" -> fg(TerminalColor.Yellow)
            "operator" -> fg(TerminalColor.Cyan)
            "comment" -> fg(TerminalColor.Default)
            "text" -> fg(TerminalColor.Default)
            "annotation" -> fg(TerminalColor.Yellow)
            "punctuation" -> fg(TerminalColor.Default)
            "string" -> fg(TerminalColor.Green)
            "raw-string" -> fg(TerminalColor.Green)
            "label" -> fg(TerminalColor.Default)
            "class-name" -> fg(TerminalColor.Yellow)
            "directive" -> fg(TerminalColor.Cyan)
            "boolean" -> fg(TerminalColor.Magenta)
            "interpolation" -> fg(TerminalColor.Red)
            "delimiter" -> fg(TerminalColor.Magenta)
            "attr-name" -> fg(TerminalColor.Yellow)
            "cdata" -> fg(TerminalColor.Red)
            "entity" -> italic + fg(TerminalColor.Red)
            "prolog" -> fg(TerminalColor.Red)
            "rule" -> italic + fg(TerminalColor.Red)
            "selector" -> fg(TerminalColor.Cyan)
            "property" -> fg(TerminalColor.Blue)
            "identifier" -> fg(TerminalColor.Default)
            "module" -> fg(TerminalColor.Green)
            "bold" -> bold
            "italic" -> italic
            else -> reset
        }

    override fun visitText(text: Prism4j.Text) {
        sb.append(reset + text.literal())
    }

    override fun visitSyntax(syntax: Prism4j.Syntax) {
        val firstChild = syntax.children().first()
        if (syntax.children().size == 1 && firstChild is Prism4j.Text) {
            sb.append(syntax.type().color + firstChild.literal() + reset)
        } else {
            visit(syntax.children())
        }
    }
}

private fun String.highlightInternal(language: String, visitor: (StringBuilder) -> AbsVisitor): String? {
    val sb = StringBuilder()
    val time = measureTime {
        val grammar = Prism.grammar(language) ?: return null
        val tokens = Prism.tokenize(this, grammar)
        visitor(sb).visit(tokens)
    }
    info("Highlighting $language (length: $length) took $time", verbose = true)
    return sb.toString()
}

/**
 * Highlight [this] according to [language], returning an HTML string.
 * If the language doesn't exist, it returns null.
 */
fun String.highlight(language: String) = highlightInternal(language) { HTMLVisitor(it, language) }

/**
 * Highlight [this] according to [language], returning an ANSI color-coded string.
 * If the language doesn't exist, it returns null.
 */
fun String.highlightANSI(language: String) = highlightInternal(language) { ANSIVisitor(it, language) }

private fun String.substr(startIndex: Int, endIndex: Int) =
    if (endIndex < 0) substring(startIndex) else substring(startIndex, endIndex)

/**
 * Run the given Kotlin script [kts] with [context].
 *
 * @param kts The script to run
 * @param context The context (global variables) to give to that script.
 * @param name The name of the script to show in diagnostic messages.
 */
fun runScript(kts: String, context: Map<String, Any?>, name: String? = null, includes: List<Path> = emptyList()): Any? {
    val fullContext = context + KtHtmlFile.globalContext
    if (kts.trim() in fullContext) {
        info("Simple replacement detected on $kts, not evaluating", verbose = true)
        return fullContext[kts.trim()].toString()
    }
    return eval(kts, fullContext, name, includes)
}

/**
 * Run the given Kotlin script [kts] with [context].
 *
 * @param kts The script to run
 * @param context The context (global variables) to give to that script.
 * @param name The name of the script to show in diagnostic messages.
 */
fun OutputPath.runScript(kts: String, context: Map<String, Any?>, name: String? = null) =
    runScript(kts, context, name, includes)

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

/**
 * Extract the frontmatter block from [srcText].
 *
 * @return the frontmatter data and the stripped text
 */
fun parseMarkdownFrontmatter(srcText: String): Pair<Map<String?, Any?>?, String> {
    if (!srcText.startsWith("```")) {
        return null to srcText
    }
    val startIndex = if (srcText.startsWith("```yaml")) "```yaml".length else "```".length
    val nextSeparator = srcText.indexOf("```", startIndex)
    if (nextSeparator == -1) {
        return null to srcText
    }

    val frontmatterText = srcText.substring(startIndex, nextSeparator - 1)
    val frontmatter = try {
        Yaml.decodeMapFromString(frontmatterText)
    } catch (e: IllegalArgumentException) {
        err("Failed to parse frontmatter $frontmatterText (${e.message})")
        null
    }

    return frontmatter to srcText.substring(nextSeparator + 4)
}

enum class TeXStyle(val intStyle: Int) {
    /**
     * The large versions of big operators are used and limits are placed under and over
     * these operators (default). Symbols are rendered in the largest size.
     */
    Display(TeXConstants.STYLE_DISPLAY),

    /**
     * The small versions of big operators are used and limits are attached to
     * these operators as scripts (default). The same size as in the display style
     * is used to render symbols.
     */
    Text(TeXConstants.STYLE_TEXT),

    /**
     * The same as the text style, but symbols are rendered in a smaller size.
     */
    Script(TeXConstants.STYLE_SCRIPT),

    /**
     * The same as the script style, but symbols are rendered in a smaller size.
     */
    SmallScript(TeXConstants.STYLE_SCRIPT_SCRIPT),
}

fun renderTeX(string: String, style: TeXStyle = TeXStyle.Display): String? {
    try {
        val formula = TeXFormula(string)
        val icon: TeXIcon = formula.createTeXIcon(style.intStyle, 20f)
        icon.setForeground(java.awt.Color.WHITE)

        val domImpl: DOMImplementation = GenericDOMImplementation.getDOMImplementation()
        val document: Document = domImpl.createDocument(null, "svg", null)
        val svgGenerator = SVGGraphics2D(SVGGeneratorContext.createDefault(document).apply {
            graphicContextDefaults = SVGGeneratorContext.GraphicContextDefaults().apply {
                paint = java.awt.Color.WHITE
            }
        }, true)

        svgGenerator.setSVGCanvasSize(Dimension(icon.iconWidth, icon.iconHeight))
        icon.paintIcon(null, svgGenerator, 0, 0)

        val writer = StringWriter()
        svgGenerator.stream(writer)
        return writer.toString().replace(
            "<svg", """<svg data-formula="${string.escapeXml()}" class="latex"""""
        )
    } catch (e: JMathTeXException) {
        val lineCount = string.lines().size
        if (lineCount == 1) {
            err("TeX formula $string failed to parse: ${e.message}")
        } else {
            err("TeX formula\n${string.prependIndent("  ")}\nfailed to parse: ${e.message}")
        }
        return null
    }
}

/**
 * Read YAML from [text] and cast it to [T]
 */
@Suppress("UNCHECKED_CAST")
fun <T> yaml(text: String): T = Yaml.decodeAnyFromString(text) as T

/**
 * Read YAML from [path] and cast it to [T]
 */
@Suppress("UNCHECKED_CAST")
fun <T> yaml(path: Path): T = Yaml.decodeAnyFromString(path.readText()) as T