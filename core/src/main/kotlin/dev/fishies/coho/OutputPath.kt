package dev.fishies.coho

import dev.fishies.coho.core.err
import dev.fishies.coho.core.scripting.eval
import dev.fishies.coho.html.html
import dev.fishies.coho.markdown.MarkdownTemplate
import dev.fishies.coho.markdown.md
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.reflect.KParameter
import kotlin.reflect.full.functions
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.host.toScriptSource

/**
 * Represents a subdirectory in the output directory.
 *
 * Arbitrary [Element]s,
 * like [md] and [html], can be nested within an
 * [OutputPath] to specify the contents of that directory.
 */
open class OutputPath(name: String, val source: Source, val buildPath: Path, var markdownTemplate: MarkdownTemplate, var includes: List<Path>) :
    Element(name) {
    val children = mutableListOf<Element>()

    override fun _generate(location: Path): List<Path> {
        val contentPath = location.resolve(name)
        contentPath.createDirectory()
        for (child in children) {
            child.generate(contentPath)
        }
        return emptyList()
    }

    override fun toString() =
        "${super.toString()}\n${children.sortedBy { executionTime }.joinToString("\n").prependIndent()}"

    /**
     * Get a path to a file in the source directory relative to this path's [source].
     */
    fun src(path: String) = source.path(path)

    /**
     * Get a path to a file in the build directory relative to this path's [source].
     */
    fun build(path: String): Path = buildPath.resolve(path)

    override val count: Int
        get() = children.sumOf { it.count }
}

/**
 * Represents a subdirectory in the output directory.
 *
 * Arbitrary [Element]s,
 * like [md] and [html], can be nested within an
 * [OutputPath] to specify the contents of that directory.
 */
fun OutputPath.path(
    name: String, markdownTemplate: MarkdownTemplate = this.markdownTemplate, includes: List<Path> = this.includes, block: OutputPath.() -> Unit
) = children.add(OutputPath(name, source.cd(name), buildPath.resolve(name), markdownTemplate, includes).apply { block() })

/**
 * Run [block] at build time.
 */
fun OutputPath.run(name: String = "run", block: OutputPath.(location: Path) -> List<Path>) =
    children.add(object : Element(name) {
        override fun _generate(location: Path): List<Path> {
            return block(location)
        }
    })

fun OutputPath.include(path: Path, vararg parameters: Pair<String?, Any?>, functionName: String = "generate") {
    RootPath.scriptSourcePath = path
    when (val result = eval(path.toFile().toScriptSource(), emptyMap())) {
        is ResultWithDiagnostics.Failure -> {}

        is ResultWithDiagnostics.Success<EvaluationResult> -> when (result.value.returnValue) {
            ResultValue.NotEvaluated -> {}
            is ResultValue.Error -> {
                val error = (result.value.returnValue as ResultValue.Error).error
                err("Evaluation resulted in error $error")
                err(error.stackTraceToString().prependIndent())
            }

            is ResultValue.Unit -> {
                val parameters = parameters.toMap() // parameter shadowing go br
                val clazz = (result.value.returnValue as ResultValue.Unit).scriptClass!!
                val instance = (result.value.returnValue as ResultValue.Unit).scriptInstance!!
                val func = clazz.functions.find { it.name == functionName }
                if (func == null) {
                    err("Couldn't find function $functionName in $path")
                    return
                }
                val passedParameters = mutableMapOf<KParameter, Any?>()
                for (param in func.parameters) {
                    if (param.kind == KParameter.Kind.INSTANCE) {
                        passedParameters[param] = instance
                        continue
                    }

                    if (param.name !in parameters) {
                        if (param.isOptional) {
                            err("Parameter ${param.name} not passed")
                        }
                        continue
                    }

                    passedParameters[param] = parameters[param.name]
                }

                val result = func.callBy(passedParameters)
                if (result !is RootPath) {
                    err("Evaluation didn't result in a RootPath")
                    return
                }
                children.addAll(result.children)
            }

            is ResultValue.Value -> {
                val result = (result.value.returnValue as ResultValue.Value).value
                if (result !is RootPath) {
                    err("Evaluation didn't result in a RootPath or Unit")
                    return
                }
                children.addAll(result.children)
            }
        }
    }
}