```yaml
meta:
  title: "index.md"
  description: "Introduction to coho, the Kotlin programmer's static website generator"
```

# index.md

Welcome to coho!

For a real-world example of coho in action, you can check
out [my website's source](https://github.com/sockeye-d/sockeye-d.github.io)

## What is coho?

Coho is a static site generator that aims to be straightforward and expressive, leveraging a custom Kotlin DSL as the
configuration language.

There are two phases to the coho build process:

1. Evaluate the main.coho.kts file
2. Build the resulting tree

In the first phase you can do literally whatever you want — it's just Kotlin.
You can write loops, read filesystem objects, and more.
In the second phase the build filetree is built to the specification of the first step, including parsing Markdown
files and emitting HTML, templating files with Kotlin, running arbitrary external programs, and more.

In the second phase you can also do literally whatever you want by using
[`run`](https://coho.fishies.dev/core/dev.fishies.coho/run.html), which will run arbitrary code as a build step
instead of an evaluation step.

## Basic coho

This is most simple `main.coho.kts` script possible:

```kotlin
root {}
```

It defines the build root and nothing else.
It's not that interesting.

You can use `cp` to copy files from the source directory to the build directory:

```kotlin
root {
    cp(src("index.html"))
}
```

As you can see, you can use `src` to reference files in the source directory.
You can also use `build` to reference files in the build directory, but that's cursed and mostly only useful for
shell scripts.

To define a subdirectory in the build directory, use `path`:

```kotlin
root {
    cp(src("index.html"))
    path("subdirectory") {
        cp(src("index.html"))
    }
}
```

As long as your build directory and source directory trees match, `src` automatically refers to files in the source
directory according to the build directory.
For example, if you had

```
main.coho.kts
index.html
subdirectory/
    index.html
```

and ran the above script, you'd get an output directory like

```
index.html
subdirectory
    index.html
```

However, if you tried to do

```kotlin
root {
    cp(src("index.html"))
    path("wrongly-named-subdirectory") {
        cp(src("index.html"))
    }
}
```

you'd get

```
index.html
wrongly-named-subdirectory/
    error: Failed to generate file ./wrongly-named-subdirectory/index.html (NoSuchFileException)
```

since the source and build directories wouldn't match.
However, you can get around this with labeled `this`es:

```kotlin
root {
    html(src("index.html"))
    path("wrongly-named-subdirectory") {
        html(this@root.src("subdirectory/index.html"))
    }
}
```

## Markdown files

This is a Markdown file.
Markdown files are converted to an HTML file in the build directory under the same name with the `.html` extension:

```kotlin
root {
    md(src("index.md"))
}
```

As you can see in the snippet above, you can use `src(path: String)` to refer to paths in the current directory.
You can also use `build(path: String)` to refer to paths in the build directory.
Note that when the coho.kts file is being evaluated, the build directory doesn't exist yet.

Markdown files are templated according to the `markdownTemplate` variable:

```kotlin
root {
    markdownTemplate = { "<html>$it</html>" }
    md(src("index.md"))
}
```

The single parameter is populated with the HTML from the Markdown.
You can access the frontmatter and other metadata by accessing the `frontmatter` variable:

```kotlin
root {
    markdownTemplate = { innerHtml ->
        val meta = frontmatter["meta"]?.asMap()
        val title: String? = meta["title"]
        """
            <!DOCTYPE HTML>
            <html>
            <head>
                <title>$title</title>
            </head>
            $innerHtml
            </html>
            """.trimIndent()
    }
    md(src("index.md"))
}
```

The frontmatter is a triple-backticked codeblock at the top of the file containing arbitrary YAML, like this:
````markdown
```yaml
meta:
  title: "example frontmatter"
  description: "a really good description"
```

# title

the rest of the document
````

The type annotation is optional, but it must either
* not exist
* be `yaml`

### Syntax highlighting

You can make a syntax-highlighted codeblock with the standard Markdown syntax:

````markdown
```kotlin
root {
    ktHtml(src("index.html"))
}
```
````

You can make syntax-highlighted inline codeblocks with this syntax:

```markdown
`#!kotlin root { ktHtml(src("index.html")) }`
```

This is good for `#!kotlin "embedding".code("in your")` text.

Coho uses [Prism4j](https://github.com/noties/Prism4j/) for syntax highlighting, so it supports the 25 languages it
does:

* `c`
* `clike`
* `clojure`
* `cpp`
* `csharp` (`dotnet`)
* `css`
* `dart`
* `git`
* `go`
* `groovy`
* `java`
* `javascript` (`js`)
* `json` (`jsonp`)
* `kotlin`
* `latex`
* `makefile`
* `markdown`
* `markup` (`xml`, `html`, `mathml`, `svg`)
* `python`
* `scala`
* `sql`
* `swift`
* `yaml`

Coho also adds definitions for

* `qml`
* `nushell`

You can syntax highlight text in Markdown files or more directly with the
[`highlight`](https://coho.fishies.dev/core/dev.fishies.coho/highlight.html) and
[`highlightANSI`](https://coho.fishies.dev/core/dev.fishies.coho/highlight-a-n-s-i.html) functions.

## HTML templating

Coho supports PHP-style templated HTML with Kotlin.
Any code within `<?kt ... ?>` will get evaluated and will be replaced with the result.

There are three primary ways to access this functionality:

* [`ktHtml`](https://coho.fishies.dev/core/dev.fishies.coho.html/kt-html.html) — works like other output types like
  `md`, processing the source file and directly outputting a plain HTML file.
* [`ktTemplate`](https://coho.fishies.dev/core/dev.fishies.coho.html/kt-template.html) — templates and returns the given
  string
* [`ktMdTemplate`](https://coho.fishies.dev/core/dev.fishies.coho.html/kt-md-template.html) — returns a function
  suitable for
  a [Markdown template](https://coho.fishies.dev/core/dev.fishies.coho.markdown/-markdown-template/index.html)

The files don't even have to be HTML files.
You can do pretty much anything with this, but here are some ideas:

### Make a list of projects

Here, templating is used to statically generate a list of projects based on the filesystem and create links to each
project in the directory.

main.coho.kts:

```kotlin
root {
    KtHTMLFile.globalContext = mapOf(
        // get a list of projects without the extensions
        "projects" to source.cd("projects").files("*.md").map { it.nameWithoutExtension },
    )

    ktHtml(src("index.html"))

    path("projects") {
        // create HTML files for each Markdown file in the projects directory
        source.files("*.md").forEach { md(src(it.name)) }
    }
}
```

index.html:

```html
<?kt
(projects as List<String>).joinToString("") { project ->
"<a class='project-link' href='/projects/$project.html'>${project.replace("-", " ")}</button></a>"
}
?>
```

Now, whatever Markdown files you put in the `projects/` directory will get auto-converted to HTML and links will be
generated for each on the main page.

## Downloading files

Coho includes support for downloading files to the build directory while optionally caching them for faster live-reload
rebuilds.

You can download a file with `dl`, seen here to download an SVG icon pack:

```kotlin
dl("https://cdn.jsdelivr.net/npm/@tabler/icons-sprite@latest/dist/tabler-sprite.svg", "tabler.svg")
```

## Interacting with external programs

There are two functions that you can use to call out to external programs:

* [`exec`](https://coho.fishies.dev/core/dev.fishies.coho/exec.html) — Run an external program at evaluation time and
  capture its stdout
* [`shell`](https://coho.fishies.dev/core/dev.fishies.coho/shell.html) — Run an external program at build time

`exec` is better suited for tasks like getting the Git hash since it runs in the source directory by default:

```kotlin
exec("git", "rev-parse", "HEAD")
```

You could even pass this into the global scripting context:

```kotlin
KtHTMLFile.globalContext = mapOf(
    "gitHash" to exec("git", "rev-parse", "--short", "HEAD"),
    "longGitHash" to exec("git", "rev-parse", "HEAD"),
)
```

and then use it in a footer:

```html

<footer>
    <span class='footer-text'>
         <!-- You can even put the templating inside strings,
              not just element contents -->
        Built from <span class='tooltipped'
                         title='<?kt longGitHash ?>'>
            <?kt gitHash ?>
        </span>
    </span>
</footer>
```

`shell` is better suited for tasks like generating a favicon with ImageMagick:

```kotlin
shell(
    "magick",
    "convert",
    "-background",
    "transparent",
    "favicon.png",
    "-define",
    "icon:auto-resize=512,16,32",
    "favicon.ico",
)
```
