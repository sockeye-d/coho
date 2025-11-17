package dev.fishies.coho.core.highlighting

import io.noties.prism4j.Prism4j.grammar
import io.noties.prism4j.Prism4j.pattern
import io.noties.prism4j.Prism4j.token
import java.util.regex.Pattern.MULTILINE
import java.util.regex.Pattern.compile

fun createIniGrammar() = grammar(
    "ini",
    token("comment", pattern(compile(";.*\\n"))),
    token("property", pattern(compile("^.+?(?==)", MULTILINE))),
    token("class-name", pattern(compile("^\\[.+\\]$", MULTILINE))),
    token("string", pattern(compile("\"(?:[^\"\\\\]|\\\\.)*+\""))),
    token("punctuation", pattern(compile("[={}\\[\\],:]"))),
)