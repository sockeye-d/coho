package dev.fishies.coho.core

import io.noties.prism4j.AbsVisitor
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

fun String.escapeHtml(): String = StringEscapeUtils.escapeHtml4(this)
fun String.escapeXml(): String = StringEscapeUtils.escapeXml11(this)

fun String.unescapeHtml(): String = StringEscapeUtils.unescapeHtml4(this)
fun String.unescapeXml(): String = StringEscapeUtils.unescapeXml(this)

fun String.highlight(language: String): String? {
    val sb = StringBuilder()
    val time = measureTime {
        val prism = Prism4j(PrismBundleGrammarLocator())
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
                    sb.append("<span class=\"code-${syntax.type()} code-$language-${syntax.type()}\">$inner</span>")
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