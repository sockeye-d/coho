package dev.fishies.coho.core.scripting

import dev.fishies.coho.core.TerminalColor
import dev.fishies.coho.core.reset
import dev.fishies.coho.core.err
import dev.fishies.coho.core.fg
import dev.fishies.coho.core.info
import dev.fishies.coho.core.note
import dev.fishies.coho.highlightANSI
import java.nio.file.Path
import kotlin.collections.iterator
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

private val ScriptDiagnostic.Severity.fgColor
    get() = when (this) {
        ScriptDiagnostic.Severity.DEBUG -> TerminalColor.Default
        ScriptDiagnostic.Severity.INFO -> TerminalColor.Default
        ScriptDiagnostic.Severity.WARNING -> TerminalColor.Yellow
        ScriptDiagnostic.Severity.ERROR -> TerminalColor.Red
        ScriptDiagnostic.Severity.FATAL -> TerminalColor.Red
    }

private fun formatDiagnostic(sourceCode: SourceCode, diagnostic: ScriptDiagnostic, printer: (String) -> Unit) =
    with(diagnostic) {
        val location = this.location
        val scriptLabel = sourcePath ?: "<anonymous>"
        if (location == null) {
            printer("$scriptLabel: $message")
            return@with
        }
        val line = sourceCode.text.highlightANSI("kotlin")!!.lines()[location.start.line - 1].trimEnd()
        val trimmedLine = line.trimStart()
        val shift = line.length - trimmedLine.length
        val column = location.start.col
        val prefix = "$scriptLabel@${location.start.line}:${location.start.col}: "
        printer("$prefix${trimmedLine}")
        val end = location.end
        val spacing = " ".repeat(prefix.length + column - shift - 1)
        if (end == null || end.line != location.start.line) {
            printer("$spacing^ $message")
        } else {
            val width = end.col - location.start.col

            // error marking graveyard kept for posterity
            // printer("${" ".repeat(prefix.length + column - 1 - shift)}${fg(Color.Red)}${"─".repeat(width - 1)}╮$RESET") // printer("${" ".repeat(prefix.length + column - 2 - shift + width)}${fg(Color.Red)}╰$RESET $message")

            // printer("${" ".repeat(prefix.length + column - 1 - shift)}${fg(Color.Red)}${"─".repeat(floor(width / 2.0).toInt())}┬${"─".repeat(ceil(width / 2.0).toInt() - 1)}$RESET")
            // printer("${" ".repeat(prefix.length + column - 1 - shift + width / 2)}${fg(Color.Red)}╰$RESET $message")

            // printer("${" ".repeat(prefix.length + column - 1 - shift)}${fg(Color.Red)}┬${"─".repeat(width - 1)}$RESET")
            // printer("${" ".repeat(prefix.length + column - 1 - shift)}${fg(Color.Red)}╰$RESET $message")

            // if (width >= 2) {
            //     printer("${" ".repeat(prefix.length + column - 1 - shift)}${fg(Color.Red)}└${"─".repeat(width - 2)}┴╴$RESET$message")
            // } else {
            //     printer("${" ".repeat(prefix.length + column - 1 - shift)}${fg(Color.Red)}^$RESET $message")
            // }

            // printer("${" ".repeat(prefix.length + column - 1 - shift)}${fg(diagnostic.severity.color)}${"^".repeat(width)}${RESET} $message")

            printer("$spacing${fg(diagnostic.severity.fgColor)}^${"~".repeat(width - 1)}${reset} $message")
        }
    }

private val host by lazy { BasicJvmScriptingHost() }

/**
 * Evaluate the Kotlin [source] with the given [context].
 * @return Whatever the script returns, or `null` if there wasn't a value.
 */
fun eval(source: SourceCode, context: Map<String, Any?>): Any? {
    val result = host.eval(source, createJvmCompilationConfigurationFromTemplate<CohoScript> {
        for ((key, value) in context) {
            val ktType = if (value == null) KotlinType(Any::class, isNullable = true) else KotlinType(value::class)
            providedProperties(key to ktType)
        }
    }, ScriptEvaluationConfiguration {
        for ((key, value) in context) {
            providedProperties(key to value)
        }
    })
    result.reports.forEach {
        when (it.severity) {
            ScriptDiagnostic.Severity.DEBUG -> formatDiagnostic(source, it) { msg -> info(msg, verbose = true) }
            ScriptDiagnostic.Severity.INFO -> formatDiagnostic(source, it, ::info)
            ScriptDiagnostic.Severity.WARNING -> formatDiagnostic(source, it, ::note)
            ScriptDiagnostic.Severity.ERROR -> formatDiagnostic(source, it, ::err)
            ScriptDiagnostic.Severity.FATAL -> formatDiagnostic(source, it, ::err)
        }
    }
    if (result.reports.any { it.severity == ScriptDiagnostic.Severity.FATAL }) {
        err("Had fatal script diagnostics")
        return null
    }
    return when (result) {
        is ResultWithDiagnostics.Failure -> {
            err("Evaluation failed")
            null
        }

        is ResultWithDiagnostics.Success<EvaluationResult> -> when (result.value.returnValue) {
            ResultValue.NotEvaluated -> null
            is ResultValue.Error -> {
                err("Evaluation resulted in error ${result.value.returnValue}")
                null
            }

            is ResultValue.Unit -> {
                err("Evaluation ended in a statement")
                null
            }

            is ResultValue.Value -> (result.value.returnValue as ResultValue.Value).value
        }
    }
}

/**
 * Evaluate the Kotlin script file at [script] with the given [context].
 * @return Whatever the script returns, or `null` if there wasn't a value.
 */
fun eval(script: Path, context: Map<String, Any?> = emptyMap()) = eval(script.toFile().toScriptSource(), context)

/**
 * Evaluate the Kotlin script string [script] with the given [context].
 * @return Whatever the script returns, or `null` if there wasn't a value.
 */
fun eval(script: String, context: Map<String, Any?> = emptyMap(), name: String? = null) =
    eval(script.toScriptSource(name), context)