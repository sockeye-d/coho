import dev.fishies.coho.core.cp
import dev.fishies.coho.markdown.md
import dev.fishies.coho.core.path
import dev.fishies.coho.core.root
import dev.fishies.coho.markdown.ProcessedMarkdownFile
import dev.fishies.coho.markdown.ogMetadataTemplate
import dev.fishies.coho.server.runLocalServer
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.name

@OptIn(ExperimentalPathApi::class)
fun main() {
    ProcessedMarkdownFile.defaultTemplate = {
        // language=html
        ogMetadataTemplate("$it\n<script src='/highlight.js' type='module'></script>")
    }

    val build = root("src") {
        //md(+"index.md")
        md(+"other.md")
        cp(+"highlight.js")
        //path("projects") {
        //    for (path in src.files("*.md")) {
        //        md(+path.name)
        //    }
        //}
    }

    //println(build)

    val buildPath = build.forceGenerate("build")
    runLocalServer(buildPath)
    build.watch() {
        build.forceGenerate("build")
    }
}