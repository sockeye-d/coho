package dev.fishies.coho.core.scripting

import dev.fishies.coho.core.err
import dev.fishies.coho.core.info
import dev.fishies.coho.core.note
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.providedProperties
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

private fun formatDiagnostic(sourceCode: Path, diagnostic: ScriptDiagnostic, printer: (String) -> Unit) =
    with(diagnostic) {
        val location = this.location
        if (location == null) {
            printer("$sourcePath: $message")
            return@with
        }
        val line = sourceCode.readText().lines()[location.start.line - 1]
        val column = location.start.col
        val prefix = "$sourcePath@${location.start.line}:${location.start.col}: "
        printer("$prefix$line")
        printer("${" ".repeat(prefix.length + column - 1)}^ $message")
    }

fun evalFile(script: Path, context: Map<String, Any?> = emptyMap()): Any? {
    val host = BasicJvmScriptingHost()
    val result = host.eval(
        script.toFile().toScriptSource(),
        createJvmCompilationConfigurationFromTemplate<CohoScript>(),
        ScriptEvaluationConfiguration {
            context.forEach { (key, value) -> providedProperties(key to value) }
        })
    result.reports.forEach {
        when (it.severity) {
            ScriptDiagnostic.Severity.DEBUG -> formatDiagnostic(script, it) { msg -> info(msg, verbose = true) }
            ScriptDiagnostic.Severity.INFO -> formatDiagnostic(script, it, ::info)
            ScriptDiagnostic.Severity.WARNING -> formatDiagnostic(script, it, ::note)
            ScriptDiagnostic.Severity.ERROR -> formatDiagnostic(script, it, ::err)
            ScriptDiagnostic.Severity.FATAL -> formatDiagnostic(script, it, ::err)
        }
    }
    if (result.reports.any { it.severity == ScriptDiagnostic.Severity.FATAL }) {
        err("Had fatal script diagnostics")
        return null
    }
    return when (result) {
        is ResultWithDiagnostics.Failure -> null
        is ResultWithDiagnostics.Success<EvaluationResult> -> when (result.value.returnValue) {
            is ResultValue.Error -> null
            ResultValue.NotEvaluated -> null
            is ResultValue.Unit -> null
            is ResultValue.Value -> (result.value.returnValue as ResultValue.Value).value
        }
    }
}