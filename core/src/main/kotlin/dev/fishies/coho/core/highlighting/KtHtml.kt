package dev.fishies.coho.core.highlighting

import io.noties.prism4j.GrammarUtils
import io.noties.prism4j.Prism4j
import io.noties.prism4j.Prism4j.*
import java.util.regex.Pattern.DOTALL
import java.util.regex.Pattern.compile

fun createKtHtmlGrammar(prism4j: Prism4j): Grammar {
    val html = prism4j.grammar("html") ?: error("HTML grammar not found")
    val kotlin = prism4j.grammar("kotlin") ?: error("Kotlin grammar not found")

    val ktHtml = grammar(
        "kthtml",
        token("preprocessor-inner", pattern(compile("(?<=\\<\\?kt).*?(?=\\?\\>)", DOTALL), false, true, "text", kotlin)),
        token("preprocessor-open-hl", pattern(compile("\\<\\?kt"), false, false, "directive")),
        token(
            "preprocessor-close-hl", pattern(compile("\\?\\>"), false, false, "directive")
        ),
        *html.tokens().toTypedArray(),
    )

    return ktHtml
}