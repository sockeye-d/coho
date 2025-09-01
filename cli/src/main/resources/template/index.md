# coho

Coho is built around the concept of nesting directories.
The `root` function declares the root build directory.
Subdirectories can be declared with the `path(name = "name")` function:

```kt
root {
    md(src("index.md"))
    path("projects") {
        html(src("index.html"))
        md(src("coho.md"))
    }
    path("blog") {
        md(src("post1.md"))
    }
}
```