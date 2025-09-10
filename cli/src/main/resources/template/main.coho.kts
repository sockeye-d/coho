root {
    markdownTemplate = {
        val meta = frontmatter["meta"]?.asMap()
        val title: String? = meta?.get("title") as? String
        val description: String? = meta?.get("description") as? String
        val type: String? = meta?.get("type") as? String

        ktMdTemplate(
            src("markdown-template.html"),
            context = mapOf("title" to title, "description" to description, "type" to type),
        )(it)
    }

    md(src("a.md"))
    md(src("index.md"))
    cp(src("style.css"))
}