package dev.fishies.coho

fun StringBuilder.cdata(configure: StringBuilder.() -> Unit = {}) {
    append("<![CDATA[")
    configure()
    append("]]>")
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
operator fun String.invoke(vararg attributes: Pair<String, Any?>, inner: StringBuilder.() -> Unit = {}) =
    with(StringBuilder()) {
        this@invoke(*attributes, inner = inner)
    }.toString()