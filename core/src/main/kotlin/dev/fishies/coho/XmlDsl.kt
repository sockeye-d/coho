package dev.fishies.coho

fun StringBuilder.cdata(configure: StringBuilder.() -> Unit = {}) {
    append("<![CDATA[")
    configure()
    append("]]>")
}

fun StringBuilder.doctype() {
    append("<!doctype html>")
}

fun StringBuilder.prolog(version: String = "1.0", encoding: String = "UTF-8") {
    append("<?xml version=\"$version\" encoding=\"$encoding\"?>")
}

/**
 * Append an XML tag with the name of [this] to [builder].
 *
 * ```kotlin
 * "html" {
 *     "body" {
 *         "h1"("class" to "hi") {
 *             append("hi")
 *         }
 *     }
 * }
 * ```
 * gives
 * ```xml
 * <html ><body ><h1 class="hi">hi</h1></body></html>
 * ```
 */
context(builder: StringBuilder) operator fun String.invoke(
    vararg attributes: Pair<String, Any?>, inner: StringBuilder.() -> Unit = {}
) {
    val attrs = attributes.map { (key, value) -> "${key.escapeXml()}=\"${value.toString().escapeXml()}\"" }
    builder.append("<$this ${attrs.joinToString(" ")}>")
    builder.inner()
    builder.append("</$this>")
}

/**
 * Create a new XML builder context.
 *
 * @see html
 * @see invoke
 */
fun xml(inner: StringBuilder.() -> Unit = {}) = StringBuilder().apply { inner() }.toString()

/**
 * Create a new XML builder context.
 *
 * @see xml
 * @see invoke
 */
fun html(inner: StringBuilder.() -> Unit = {}) = StringBuilder().apply { inner() }.toString()