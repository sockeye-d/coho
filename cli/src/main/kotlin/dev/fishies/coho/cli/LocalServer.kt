package dev.fishies.coho.cli

import dev.fishies.coho.core.POSITIVE
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.StateFlow
import java.nio.file.Path
import kotlin.io.path.absolute

// language=javascript
const val RELOAD_JS = """
const reload = new WebSocket("/reload");
reload.addEventListener('message', event => {
    console.log("reloading");
    location.reload();
});
"""

private val endHtmlRegex = Regex("<\\s*?/\\s*?[hH][tT][mM][lL]\\s*?>")

private fun injectReloadJs(html: String): String {
    val endHtmlIndex = endHtmlRegex.find(html)?.groups?.get(0)?.range?.start
    if (endHtmlIndex == null) {
        return html
    }
    // language=html
    return "${html.substring(0, endHtmlIndex)}<script>$RELOAD_JS</script>${html.substring(endHtmlIndex)}"
}

fun runLocalServer(buildPath: Path, reload: StateFlow<Int>, noReloadScript: Boolean, port: Int = 8080) =
    embeddedServer(Netty, port, host = "127.0.0.1") {
        install(WebSockets)
        routing {
            webSocket("/reload") {
                var lastReloadState: Int? = null
                reload.collect {
                    if (lastReloadState == null) {
                        lastReloadState = it
                    } else if (lastReloadState != it) {
                        lastReloadState = it
                        send("reload please")
                    }
                }
            }
            staticFiles("/", buildPath.absolute().toFile()) {
                if (noReloadScript) {
                    return@staticFiles
                }
                modify { file, call ->
                    if (file.extension == "html") {
                        call.respondText(injectReloadJs(file.readText()), ContentType.Text.Html)
                    }
                }
            }
        }
    }.start()
