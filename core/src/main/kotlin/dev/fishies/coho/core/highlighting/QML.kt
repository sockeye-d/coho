package dev.fishies.coho.core.highlighting

import io.noties.prism4j.Prism4j
import io.noties.prism4j.Prism4j.*
import java.util.regex.Pattern.DOTALL
import java.util.regex.Pattern.compile

// language=regexp
private const val IDENTIFIER = "[a-zA-Z_][a-zA-Z0-9-_]*"

// language=regexp
private const val QUALIFIED_IDENTIFIER = "($IDENTIFIER\\.)*$IDENTIFIER"

private const val TYPED_IDENTIFIER = "$IDENTIFIER\\s*"
private const val TYPED_ARGUMENT_LIST = "($TYPED_IDENTIFIER?)|($TYPED_IDENTIFIER\\s*,\\s*)"
private val brackets = generateBracketPattern()

private fun generateBracketPattern(depth: Int = 4): String {
    fun level(currentDepth: Int): String =
        if (currentDepth <= 0) "[^{}]*" else "(?:[^{}]|\\{${level(currentDepth - 1)}\\})*"
    return level(depth)
}

/**
 * @suppress
 */
fun createQmlGrammar(prism4j: Prism4j): Grammar {
    val javascript = prism4j.grammar("javascript") ?: error("JavaScript grammar not found")

    val qml = grammar(
        "qml",
        token(
            "comment", pattern(compile("//.*|/\\*[\\s\\S]*?\\*/"), false, true)
        ),
        token(
            "string", pattern(compile("\"[^\"]*\""), false, true), pattern(compile("\'[^\']*\'"), false, true)
        ),

        token(
            "obj-property",
            pattern(
                compile("$IDENTIFIER\\s*:\\s*$QUALIFIED_IDENTIFIER\\s*\\{"), false, false, null, grammar(
                    "obj-property-inside",
                    token("property", pattern(compile("^$IDENTIFIER"))),
                    token("module", pattern(compile("$IDENTIFIER(?=\\s*\\.)"))),
                    token("class-name", pattern(compile(IDENTIFIER))),
                    token("punctuation", pattern(compile("[.:{}]"))),
                )
            ),
        ),
        token(
            "js-multiline-property",
            pattern(
                compile("(?<=$IDENTIFIER\\s*:\\s*\\{)$brackets(?=\\})", DOTALL), false, true, null, javascript
            ),
        ),
        token(
            "js-property",
            pattern(
                compile("$IDENTIFIER(?=\\s*:\\s*.*[\\n;])"), false, true, "property", javascript
            ),
        ),
        token(
            "js-unmatched",
            pattern(
                compile("(?<=$IDENTIFIER\\s*:\\s*).*(?=[\\n;])"), false, true, "identifier", javascript
            ),
        ),
        token(
            "js-function", pattern(compile("(?<=function\\s+).*$brackets"), false, true, "function", javascript)
        ),

        token(
            "property-alias-declaration-type",
            pattern(compile("(?<=property\\s+)alias(?=\\s+$IDENTIFIER)"), false, false, "keyword")
        ),
        token(
            "property-declaration-type", pattern(
                compile("(?<=property\\s+)$IDENTIFIER(?=\\s+$IDENTIFIER)"), false, false, "class-name"
            )
        ),
        token(
            "property-declaration", pattern(
                compile("(?<=property\\s+$IDENTIFIER\\s+)$IDENTIFIER"), false, true, "property"
            )
        ),

        token(
            "signal-declaration", pattern(
                compile("(?<=signal\\s+)$IDENTIFIER(?=\\($TYPED_ARGUMENT_LIST\\))"), false, true, "property"
            )
        ),
        token(
            "signal-declaration", pattern(
                compile("(?<=signal\\s+)$IDENTIFIER(?=\\((${TYPED_IDENTIFIER}?)|(${TYPED_IDENTIFIER}\\s*,\\s*)\\))"),
                false,
                true,
                "property"
            )
        ),

        token(
            "keyword",
            pattern(compile("import|alias|as|true|false|default|property|var|readonly|signal|function|double|real|pragma"))
        ),
        token("punctuation", pattern(compile("[.:{}]"), false, false)),
        token("module", pattern(compile("$IDENTIFIER(?=\\s*\\.)"), false, true)),
        token("class-name", pattern(compile(IDENTIFIER))),
    )

    return qml
}