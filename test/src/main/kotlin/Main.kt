import dev.fishies.coho.core.cp
import dev.fishies.coho.core.ktHtml
import dev.fishies.coho.markdown.md
import dev.fishies.coho.core.path
import dev.fishies.coho.core.root
import dev.fishies.coho.markdown.ProcessedMarkdownFile
import dev.fishies.coho.markdown.ogMetadataTemplate
import dev.fishies.coho.server.runLocalServer
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

fun build() = root("src") {
    val ctx = mapOf("projects" to src.cd("projects").files("*.md").map(Path::nameWithoutExtension))
    md(+"index.md")
    md(+"other.md")
    ktHtml(+"thingy.html", ctx)
    cp(+"highlight.js")
    path("projects") {
        for (path in src.files("*.md")) {
            md(+path.name)
        }
    }
}

@OptIn(ExperimentalPathApi::class)
fun main() {
    ProcessedMarkdownFile.defaultTemplate = {
        // language=html
        ogMetadataTemplate("$it\n<script src='/highlight.js' type='module'></script>")
    }

    var build = build()

    val buildPath = build.forceGenerate("build")
    runLocalServer(buildPath)
    build.watch() {
        build = build()
        build.forceGenerate("build")
    }
}