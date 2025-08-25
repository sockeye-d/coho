import dev.fishies.coho.core.md
import dev.fishies.coho.core.path
import dev.fishies.coho.core.root
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.name

@OptIn(ExperimentalPathApi::class)
fun main() {
    val build = root("src") {
        md(+"index.md")
        md(+"other.md")
        path("projects") {
            for (path in src.files("*.md")) {
                md(+path.name)
            }
        }
    }

    println(build)

    build.forceGenerate("build")
}