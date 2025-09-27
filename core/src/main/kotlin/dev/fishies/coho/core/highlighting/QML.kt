package dev.fishies.coho.core.highlighting

import io.noties.prism4j.Prism4j
import io.noties.prism4j.Prism4j.*
import java.util.regex.Pattern.DOTALL
import java.util.regex.Pattern.compile

// language=regexp
private const val qmlIdentifier = "[a-zA-Z_][a-zA-Z0-9-_]*"

// language=regexp
private const val qualifiedIdentifier = "($qmlIdentifier\\.)*$qmlIdentifier"

private const val typedIdentifier = "$qmlIdentifier\\s*"
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
                compile("$qmlIdentifier\\s*:\\s*$qualifiedIdentifier\\s*\\{"), false, false, null, grammar(
                    "obj-property-inside",
                    token("property", pattern(compile("^$qmlIdentifier"))),
                    token("module", pattern(compile("$qmlIdentifier(?=\\s*\\.)"))),
                    token("class-name", pattern(compile(qmlIdentifier))),
                    token("punctuation", pattern(compile("[.:{}]"))),
                )
            ),
        ),
        token(
            "js-multiline-property",
            pattern(
                compile("(?<=$qmlIdentifier\\s*:\\s*\\{)$brackets(?=\\})", DOTALL), false, true, null, javascript
            ),
        ),
        token(
            "js-property",
            pattern(
                compile("$qmlIdentifier(?=\\s*:\\s*.*[\\n;])"), false, true, "property", javascript
            ),
        ),
        token(
            "js-unmatched",
            pattern(
                compile("(?<=$qmlIdentifier\\s*:\\s*).*(?=[\\n;])"), false, true, "identifier", javascript
            ),
        ),
        token(
            "js-function", pattern(compile("(?<=function\\s+).*$brackets"), false, true, "function", javascript)
        ),

        token(
            "property-alias-declaration-type",
            pattern(compile("(?<=property\\s+)alias(?=\\s+$qmlIdentifier)"), false, false, "keyword")
        ),
        token(
            "property-declaration-type", pattern(
                compile("(?<=property\\s+)$qmlIdentifier(?=\\s+$qmlIdentifier)"), false, false, "class-name"
            )
        ),
        token(
            "property-declaration", pattern(
                compile("(?<=property\\s+$qmlIdentifier\\s+)$qmlIdentifier"), false, true, "property"
            )
        ),

        token(
            "signal-declaration", pattern(
                compile("(?<=signal\\s+)$qmlIdentifier(?=\\($typedArgList\\))"), false, true, "property"
            )
        ),
        token(
            "signal-declaration", pattern(
                compile("(?<=signal\\s+)$qmlIdentifier(?=\\((${typedIdentifier}?)|(${typedIdentifier}\\s*,\\s*)\\))"),
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
        token("module", pattern(compile("$qmlIdentifier(?=\\s*\\.)"), false, true)),
        token("class-name", pattern(compile(qmlIdentifier))),
    )

    return qml
}