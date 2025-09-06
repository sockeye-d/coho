package dev.fishies.coho.core

import io.noties.prism4j.Prism4j
import io.noties.prism4j.Prism4j.*
import io.noties.prism4j.annotations.PrismBundle
import java.util.regex.Pattern.compile

@Suppress("unused")
@PrismBundle(
    includeAll = true, grammarLocatorClassName = "dev.fishies.coho.core.PrismBundleGrammarLocator",
)
class WhyDoesThisExist

@Suppress("unused")
object Prism_qml {
    fun create(prism4j: Prism4j): Grammar {
        val javascript = prism4j.grammar("javascript")

        val qml = grammar(
            "qml",  // Comments
            token(
                "comment", pattern(compile("//.*|/\\*[\\s\\S]*?\\*/"), false, true)
            ),  // Pragma directive
        )

        return qml
    }
}