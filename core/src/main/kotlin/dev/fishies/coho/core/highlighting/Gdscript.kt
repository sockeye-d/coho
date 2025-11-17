package dev.fishies.coho.core.highlighting

import io.noties.prism4j.Prism4j.*
import java.util.regex.Pattern.compile

private const val identifier = "[a-zA-Z_][a-zA-Z0-9_]*"
private val keywords = listOf(
    "and",
    "if",
    "elif",
    "else",
    "for",
    "while",
    "break",
    "continue",
    "pass",
    "return",
    "match",
    "when",
    "as",
    "assert",
    "await",
    "breakpoint",
    "class",
    "class_name",
    "const",
    "enum",
    "extends",
    "func",
    "in",
    "is",
    "not",
    "not in",
    "null",
    "or",
    "is not",
    "namespace",
    "preload",
    "self",
    "signal",
    "static",
    "super",
    "trait",
    "var",
    "yield",
).sortedBy { it.length }.joinToString("|")
private val variantTypes = listOf(
    "int",
    "bool",
    "float",
    "void",
).sortedBy { it.length }.joinToString("|")

fun createGdscriptGrammar() = grammar(
    "gdscript",
    token("comment", pattern(compile("#.*\\n"))),
    token("double-quote-string", pattern(compile("&?\\^?r?\"(?:[^\"\\\\]|\\\\.)*+\""), false, true, "string")),
    token("single-quote-string", pattern(compile("&?\\^?r?\'(?:[^\'\\\\]|\\\\.)*+\'"), false, true, "string")),
    token("triple-double-quote-string", pattern(compile("r?(\"{3,}).*?\\1"), false, true, "string")),
    token("triple-single-quote-string", pattern(compile("r?(\'{3,}).*?\\1"), false, true, "string")),
    token("annotation", pattern(compile("@$identifier"), false, true)),
    token(
        "class-def", pattern(
            compile("(?<=class_name|class)\\s+$identifier\\s+((?=extends)\\s+$identifier)?"), false, false, "class-name"
        )
    ),
    token(
        "number",
        pattern(compile("-?(?:0[xX](?:[0-9a-fA-F]_?)+|0[bB](?:[01]_?)+|(?:[0-9]_?)*\\.(?:[0-9]_?)+(?:[eE]-?(?:[0-9]_?)+)?|(?:[0-9]_?)+\\.?(?:[eE]-?(?:[0-9]_?)+)?)"))
    ),
    token("operator", pattern(compile("=|\\+=|-=|\\*\\*=|\\*=|/=|%=|&=|\\^=|<<=|>>=|:\\s*=|->"))),
    token(
        "assignment",
        pattern(compile("\\*\\*|~|\\+|-|\\*|/|%|>>|<<|&|\\^|\\||[=!]=|[<>]=?|!"), false, false, "operator")
    ),
    token("constant", pattern(compile("PI|TAU|NAN|INF"))),
    token("boolean", pattern(compile("true|false"))),
    token("keyword", pattern(compile("\\b(?:$keywords)\\b"))),
    token("constant", pattern(compile("\\b[A-Z_][A-Z0-9_]*\\b"), false, true)),
    token("pascal-class-name", pattern(compile("\\b[A-Z][a-zA-Z0-9_]*\\b"), false, true, "class-name")),
    token("variant-class-name", pattern(compile("\\b(?:$variantTypes)\\b"), false, true, "class-name")),
    token("function", pattern(compile("$identifier(?=\\s*\\()"))),
    token("punctuation", pattern(compile("[:.,(){}\\[\\]]"))),
)