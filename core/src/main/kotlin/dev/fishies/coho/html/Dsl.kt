package dev.fishies.coho.html

import dev.fishies.coho.escapeXml

fun StringBuilder.tag(name: String, vararg attributes: Pair<String, Any?>, configure: StringBuilder.() -> Unit = {}) {
    val attrs = attributes.map { (key, value) -> "${key.escapeXml()}=\"${value.toString().escapeXml()}\"" }
    append("<$name ${attrs.joinToString(" ")}>")
    configure()
    append("</$name>")
}

fun StringBuilder.cdata(configure: StringBuilder.() -> Unit = {}) {
    append("<![CDATA[")
    configure()
    append("]]>")
}

fun tag(name: String, vararg attributes: Pair<String, Any?>, configure: StringBuilder.() -> Unit = {}): String {
    val sb = StringBuilder()
    sb.tag(name, *attributes, configure = configure)
    return sb.toString()
}
