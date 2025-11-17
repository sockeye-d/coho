package dev.fishies.coho.core.highlighting

import io.noties.prism4j.Prism4j.grammar
import io.noties.prism4j.Prism4j.pattern
import io.noties.prism4j.Prism4j.token
import java.util.regex.Pattern.MULTILINE
import java.util.regex.Pattern.compile

private val variantTypes = listOf(
    "int",
    "bool",
    "float",
    "void",
).sortedBy { it.length }.joinToString("|")

fun createIniGrammar() = grammar(
    "ini",
    token("comment", pattern(compile(";.*\\n"))),
    token("property", pattern(compile("^.+?(?==)", MULTILINE))),
    token("class-name", pattern(compile("^\\[.+\\]$", MULTILINE))),
    token("string", pattern(compile("&?\\^?\"(?:[^\"\\\\]|\\\\.)*+\""))),
    token("boolean", pattern(compile("true|false"))),
    token(
        "number",
        pattern(compile("-?(?:0[xX](?:[0-9a-fA-F]_?)+|0[bB](?:[01]_?)+|(?:[0-9]_?)*\\.(?:[0-9]_?)+(?:[eE]-?(?:[0-9]_?)+)?|(?:[0-9]_?)+\\.?(?:[eE]-?(?:[0-9]_?)+)?)"))
    ),
    token("pascal-class-name", pattern(compile("\\b[A-Z][a-zA-Z0-9_]*\\b"), false, true, "class-name")),
    token("variant-class-name", pattern(compile("\\b(?:$variantTypes)\\b"), false, true, "class-name")),
    token("punctuation", pattern(compile("[=,:{}\\[\\]()]"))),
)