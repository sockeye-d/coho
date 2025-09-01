import dev.fishies.coho.core.*
// plain html and html templating functions
import dev.fishies.coho.core.html.*
// markdown functions like `md` and `basicMd`
import dev.fishies.coho.core.markdown.*
import kotlin.io.path.*

root {
    md(src("index.md"))
}