package dev.fishies.coho.core.scripting

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

@KotlinScript(
    displayName = "Coho script", fileExtension = "coho.kts", compilationConfiguration = CohoScriptConfiguration::class
)
abstract class CohoScript

class CohoScriptConfiguration : ScriptCompilationConfiguration({
    defaultImports(
        "dev.fishies.coho.core.*",
        "dev.fishies.coho.core.markdown.*",
        "dev.fishies.coho.core.html.*",
        "java.nio.file.Path",
        "kotlin.io.path.*",
    )
    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
    }
    ide {
        acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }
})
