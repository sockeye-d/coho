package dev.fishies.coho.core.scripting

import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

fun evalFile(script: String) = BasicJvmScriptingHost().eval(
    script.toScriptSource("coho script"), createJvmCompilationConfigurationFromTemplate<CohoScript>(), null
)