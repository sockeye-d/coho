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
        else -> delegate.grammar(prism4j, language)
    }

    override fun languages() = delegate.languages() + "qml"
})