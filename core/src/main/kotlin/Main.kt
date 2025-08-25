import dev.fishies.coho.core.Source
import dev.fishies.coho.core.html
import dev.fishies.coho.core.md
import dev.fishies.coho.core.page
import dev.fishies.coho.core.root
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteRecursively

@OptIn(ExperimentalPathApi::class)
fun main() {
    val x = root(Source("src")) {
        md(+"index.md")
        page("projects") {
            md(+"godl.md")
            md(+"sled.md")
            md(+"coho.md")
        }
    }

    println(x)

    Paths.get("build").deleteRecursively()
    x.generate("build")
}