package dev.fishies.coho.core.scripting

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

/**
 * @suppress
 */
@KotlinScript(
    displayName = "Coho script", fileExtension = "coho.kts", compilationConfiguration = CohoScriptConfiguration::class
)
abstract class CohoScript

/**
 * @suppress
 */
class CohoScriptConfiguration : ScriptCompilationConfiguration({
    defaultImports(
        "dev.fishies.coho.*",
        "dev.fishies.coho.markdown.*",
        "dev.fishies.coho.html.*",
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
