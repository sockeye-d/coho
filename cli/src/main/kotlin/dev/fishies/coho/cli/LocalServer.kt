package dev.fishies.coho.cli

import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import java.nio.file.Path
import kotlin.io.path.absolute

fun runLocalServer(path: Path, port: Int = 8001) {
    println("Running local server on http://127.0.0.1:$port")
    embeddedServer(Netty, port, host = "127.0.0.1") {
        routing {
            staticFiles("/", path.absolute().toFile())
        }
    }.start()
}