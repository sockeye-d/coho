package dev.fishies.coho.core.scripting

import dev.fishies.coho.OutputPath
import dev.fishies.coho.core.*
import dev.fishies.coho.highlightANSI
import java.nio.file.Path
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class Import

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
        val lines = sourceCode.text.highlightANSI("kotlin")!!.lines()
        if (location.start.line >= lines.size) {
            printer("$scriptLabel: $message")
            return@with
        }
        val line = lines[location.start.line - 1].trimEnd()
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
            printer("$spacing${fg(diagnostic.severity.fgColor)}^${if (width == 0) "" else "~".repeat(width - 1)}${reset} $message")
        }
    }

private val host by lazy { BasicJvmScriptingHost() }

private fun unwrapResult(result: ResultWithDiagnostics<EvaluationResult>): Any? = when (result) {
    is ResultWithDiagnostics.Failure -> {
        null
    }

    is ResultWithDiagnostics.Success<EvaluationResult> -> when (result.value.returnValue) {
        ResultValue.NotEvaluated -> null
        is ResultValue.Error -> {
            val error = (result.value.returnValue as ResultValue.Error).error
            err("Evaluation resulted in error $error")
            err(error.stackTraceToString().prependIndent())
            null
        }

        is ResultValue.Unit -> {
            err("Evaluation ended in a statement")
            null
        }

        is ResultValue.Value -> (result.value.returnValue as ResultValue.Value).value
    }
}

/**
 * Evaluate the Kotlin [source] with the given [context].
 * @return The result of evaluation
 */
fun eval(
    source: SourceCode, context: Map<String, Any?>, includes: List<Path> = emptyList()
): ResultWithDiagnostics<EvaluationResult> {
    val result = BasicJvmScriptingHost().eval(source, createJvmCompilationConfigurationFromTemplate<CohoScript> {
        refineConfiguration {
            onAnnotations(Import::class, handler = CohoImportAnnotationConfigurator(includes))
        }

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
        return result
    }
    return result
}

private class CohoImportAnnotationConfigurator(private val includes: List<Path>) :
    RefineScriptCompilationConfigurationHandler {
    override fun invoke(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        return ScriptCompilationConfiguration(context.compilationConfiguration) {
            this[importScripts] = includes.distinct().map { it.toFile().toScriptSource() }
        }.asSuccess()
    }
}

/**
 * Evaluate the Kotlin script file at [script] with the given [context].
 * @return Whatever the script returns, or `null` if there wasn't a value.
 */
fun eval(script: Path, context: Map<String, Any?> = emptyMap(), includes: List<Path> = emptyList()) =
    unwrapResult(eval(script.toFile().toScriptSource(), context, includes))

/**
 * Evaluate the Kotlin script file at [script] with the given [context].
 * @return Whatever the script returns, or `null` if there wasn't a value.
 */
fun OutputPath.eval(script: Path, context: Map<String, Any?> = emptyMap()) =
    unwrapResult(eval(script.toFile().toScriptSource(), context, includes))

/**
 * Evaluate the Kotlin script string [script] with the given [context].
 * @return Whatever the script returns, or `null` if there wasn't a value.
 */
fun eval(
    script: String, context: Map<String, Any?> = emptyMap(), name: String? = null, includes: List<Path> = emptyList()
) = unwrapResult(eval(script.toScriptSource(name), context, includes))

/**
 * Evaluate the Kotlin script string [script] with the given [context].
 * @return Whatever the script returns, or `null` if there wasn't a value.
 */
fun OutputPath.eval(
    script: String, context: Map<String, Any?> = emptyMap(), name: String? = null
) = unwrapResult(eval(script.toScriptSource(name), context, includes))