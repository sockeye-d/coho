package dev.fishies.coho.core.scripting

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

@KotlinScript(
    displayName = "Coho script",
    fileExtension = "coho.kts",
    filePathPattern = "*.coho.kts",
    compilationConfiguration = CohoScriptConfiguration::class
)

abstract class CohoScript

@Suppress("JavaIoSerializableObjectMustHaveReadResolve")
object CohoScriptConfiguration : ScriptCompilationConfiguration({
    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
    }
})
