package dev.fishies.coho.core.highlighting

import dev.fishies.coho.core.PrismBundleGrammarLocator
import io.noties.prism4j.GrammarLocator
import io.noties.prism4j.Prism4j
import io.noties.prism4j.annotations.PrismBundle

/**
 * @suppress
 */
@Suppress("unused")
@PrismBundle(
    includeAll = true, grammarLocatorClassName = "dev.fishies.coho.core.PrismBundleGrammarLocator",
)
class WhyDoesThisExist

val prism = Prism4j(object : GrammarLocator {
    val delegate = PrismBundleGrammarLocator()
    override fun grammar(prism4j: Prism4j, language: String) = when (language) {
        "qml" -> createQmlGrammar(prism4j)
        "kthtml" -> createKtHtmlGrammar(prism4j)
        in arrayOf("nu", "nushell") -> createNushellGrammar()
        else -> delegate.grammar(prism4j, language)
    }

    override fun languages() = delegate.languages() + "qml"
})

/**
 * @suppress
 */
fun generateNestedPattern(depth: Int = 4, opening: String = "{", closing: String = "}"): String {
    fun level(currentDepth: Int): String =
        if (currentDepth <= 0) "[^$opening]*" else "(?:[^$opening]|$opening${level(currentDepth - 1)}$closing)*"
    return level(depth)
}