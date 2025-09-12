package dev.fishies.coho.core.highlighting

import io.noties.prism4j.Prism4j
import io.noties.prism4j.Prism4j.*
import java.util.regex.Pattern.DOTALL
import java.util.regex.Pattern.compile

// language=regexp
private const val identifier = "[a-zA-Z_][a-zA-Z0-9-_]*"

// language=regexp
private const val qualifiedIdentifier = "($identifier\\.)*$identifier"

private const val typedIdentifier = "$identifier\\s*"
private const val typedArgList = "($typedIdentifier?)|($typedIdentifier\\s*,\\s*)"
private val brackets = generateNestedPattern()

object Prism_qml
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
                compile("$identifier\\s*:\\s*$qualifiedIdentifier\\s*\\{"), false, false, null, grammar(
                    "obj-property-inside",
                    token("property", pattern(compile("^$identifier"))),
                    token("module", pattern(compile("$identifier(?=\\s*\\.)"))),
                    token("class-name", pattern(compile(identifier))),
                    token("punctuation", pattern(compile("[.:{}]"))),
                )
            ),
        ),
        token(
            "js-multiline-property",
            pattern(
                compile("(?<=$identifier\\s*:\\s*\\{)$brackets(?=\\})", DOTALL), false, true, null, javascript
            ),
        ),
        token(
            "js-property",
            pattern(
                compile("$identifier(?=\\s*:\\s*.*[\\n;])"), false, true, "property", javascript
            ),
        ),
        token(
            "js-unmatched",
            pattern(
                compile("(?<=$identifier\\s*:\\s*).*(?=[\\n;])"), false, true, "identifier", javascript
            ),
        ),
        token(
            "js-function", pattern(compile("(?<=function\\s+).*$brackets"), false, true, "function", javascript)
        ),

        token(
            "property-alias-declaration-type",
            pattern(compile("(?<=property\\s+)alias(?=\\s+$identifier)"), false, false, "keyword")
        ),
        token(
            "property-declaration-type", pattern(
                compile("(?<=property\\s+)$identifier(?=\\s+$identifier)"), false, false, "class-name"
            )
        ),
        token(
            "property-declaration", pattern(
                compile("(?<=property\\s+$identifier\\s+)$identifier"), false, true, "property"
            )
        ),

        token(
            "signal-declaration", pattern(
                compile("(?<=signal\\s+)$identifier(?=\\($typedArgList\\))"), false, true, "property"
            )
        ),
        token(
            "signal-declaration", pattern(
                compile("(?<=signal\\s+)$identifier(?=\\((${typedIdentifier}?)|(${typedIdentifier}\\s*,\\s*)\\))"),
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
        token("module", pattern(compile("$identifier(?=\\s*\\.)"), false, true)),
        token("class-name", pattern(compile(identifier))),
    )

    return qml
}