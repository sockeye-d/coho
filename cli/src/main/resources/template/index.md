```yaml
meta:
  title: "index.md"
```

# index.md

Welcome to coho!

## What is coho?

Coho is a static site generator that aims to be straightforward and expressive, leveraging a custom Kotlin DSL as the
configuration language.

There are two phases to the coho build process:
1. Evaluate the coho.kts file
2. Build the resulting tree

In the first phase you can do literally whatever you want — it's just Kotlin.
You can write loops, read filesystem objects, and more.
In the second phase the build filetree is built to the specification of the first step, including parsing Markdown 
files and emitting HTML, templating files with Kotlin, running arbitrary external programs, and more.

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

# HTML templating

Coho supports PHP-style templated HTML with Kotlin. Any code within `<?kt ... ?>` will get evaluated and will be 
replaced with the result.

There are three primary ways to access this:
* [`ktHtml`](https://coho.fishies.dev/core/dev.fishies.coho.html/kt-html.html) — works like other output types like 
  `md`, processing the source file and directly outputting a plain HTML file.
* [`ktMdTemplate`](https://coho.fishies.dev/core/dev.fishies.coho.html/kt-md-template.html)  — works 
* `ktTemplate`