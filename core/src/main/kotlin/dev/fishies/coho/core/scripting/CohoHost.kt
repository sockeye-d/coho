package dev.fishies.coho.core.scripting

import dev.fishies.coho.core.Color
import dev.fishies.coho.core.RESET
import dev.fishies.coho.core.err
import dev.fishies.coho.core.fg
import dev.fishies.coho.core.info
import dev.fishies.coho.core.note
import java.nio.file.Path
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.providedProperties
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

val ScriptDiagnostic.Severity.color
    get() = when (this) {
        ScriptDiagnostic.Severity.DEBUG -> Color.DEFAULT
        ScriptDiagnostic.Severity.INFO -> Color.DEFAULT
        ScriptDiagnostic.Severity.WARNING -> Color.YELLOW
        ScriptDiagnostic.Severity.ERROR -> Color.RED
        ScriptDiagnostic.Severity.FATAL -> Color.RED
    }

private fun formatDiagnostic(sourceCode: SourceCode, diagnostic: ScriptDiagnostic, printer: (String) -> Unit) =
    with(diagnostic) {
        val location = this.location
        val scriptLabel = sourcePath ?: "<anonymous>"
        if (location == null) {
            printer("$scriptLabel: $message")
            return@with
        }
        val line = sourceCode.text.lines()[location.start.line - 1].trimEnd()
        val trimmedLine = line.trimStart()
        val shift = line.length - trimmedLine.length
        val column = location.start.col
        val prefix = "$scriptLabel@${location.start.line}:${location.start.col}: "
        printer("$prefix$trimmedLine")
        val end = location.end
        if (end == null || end.line != location.start.line) {
            printer("${" ".repeat(prefix.length + column - 1 - shift)}╰ $message")
        } else {
            val width = (end.col - location.start.col)
            // printer("${" ".repeat(prefix.length + column - 1 - shift)}${fg(Color.Red)}${"─".repeat(width - 1)}╮$RESET")
            // printer("${" ".repeat(prefix.length + column - 2 - shift + width)}${fg(Color.Red)}╰$RESET $message")

            // printer("${" ".repeat(prefix.length + column - 1 - shift)}${fg(Color.Red)}${"─".repeat(floor(width / 2.0).toInt())}┬${"─".repeat(ceil(width / 2.0).toInt() - 1)}$RESET")
            // printer("${" ".repeat(prefix.length + column - 1 - shift + width / 2)}${fg(Color.Red)}╰$RESET $message")

            // printer("${" ".repeat(prefix.length + column - 1 - shift)}${fg(Color.Red)}┬${"─".repeat(width - 1)}$RESET")
            // printer("${" ".repeat(prefix.length + column - 1 - shift)}${fg(Color.Red)}╰$RESET $message")

            // if (width >= 2) {
            //     printer("${" ".repeat(prefix.length + column - 1 - shift)}${fg(Color.Red)}└${"─".repeat(width - 2)}┴╴$RESET$message")
            // } else {
            //     printer("${" ".repeat(prefix.length + column - 1 - shift)}${fg(Color.Red)}^$RESET $message")
            // }

            printer("${" ".repeat(prefix.length + column - 1 - shift)}${fg(diagnostic.severity.color)}${"^".repeat(width)}${RESET} $message")
        }
    }

private val host by lazy { BasicJvmScriptingHost() }

private fun evalSource(source: SourceCode, context: Map<String, Any?>): Any? {
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
                err("Evaluation resulted in error ${(result.value.returnValue as ResultValue.Error).error}")
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

fun eval(script: Path, context: Map<String, Any?> = emptyMap()) = evalSource(script.toFile().toScriptSource(), context)

fun eval(script: String, context: Map<String, Any?> = emptyMap(), name: String? = null) =
    evalSource(script.toScriptSource(name), context)