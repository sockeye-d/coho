package dev.fishies.coho.core.highlighting

import io.noties.prism4j.Prism4j
import io.noties.prism4j.Prism4j.*
import java.util.regex.Pattern.CASE_INSENSITIVE
import java.util.regex.Pattern.compile

// language=regexp
const val identifier = "[\\w-]+"
fun createMarkupGrammar(prism4j: Prism4j): Grammar {
    val entity = token("entity", pattern(compile("&\\w*;"), false, true))
    val insideTag = grammar(
        "inside-tag",
        token("tag", pattern(compile("(?<=^<\\/?\\s*)$identifier"), false, false, "property")),
        token("attr-name", pattern(compile("$identifier(?=\\s*=)"))),
        token("string", pattern(compile("(?<=$identifier\\s*=)(?:\".*?\"|.+?\\b)"))),
        token("open", pattern(compile("^<\\/?"), false, true, "punctuation")),
        token("close", pattern(compile("\\/?>$"), false, true, "punctuation")),
    )
    val markup = grammar(
        "markup",
        token("comment", pattern(compile("<!--[\\s\\S]*?-->"))),
        token("prolog", pattern(compile("<\\?[\\s\\S]+?\\?>"))),
        token("doctype", pattern(compile("<!DOCTYPE[\\s\\S]+?>", CASE_INSENSITIVE))),
        token("cdata", pattern(compile("<!\\[CDATA\\[[\\s\\S]*?]]>", CASE_INSENSITIVE))),
        token(
            "tag", pattern(
                compile(
                    "<\\/?(?!\\d)[^\\s>\\/=$<%]+(?:\\s+[^\\s>\\/=]+(?:=(?:([\"'])(?:\\\\[\\s\\S]|(?!\\1)[^\\\\])*\\1|[^\\s'\">=]+))?)*\\s*\\/?>",
                    CASE_INSENSITIVE
                ), false, true, null, insideTag
            )
        ),
        entity,
    )

    return markup
}