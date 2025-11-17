package dev.fishies.coho.core.highlighting

import dev.fishies.coho.core.PrismBundleGrammarLocator
import dev.fishies.coho.core.err
import dev.fishies.coho.core.highlighting.Prism.extraGrammars
import io.noties.prism4j.*
import io.noties.prism4j.annotations.PrismBundle
import java.awt.SystemColor.text
import java.util.regex.Matcher

/**
 * @suppress
 */
@Suppress("unused")
@PrismBundle(
    includeAll = true, grammarLocatorClassName = "dev.fishies.coho.core.PrismBundleGrammarLocator",
)
private class WhyDoesThisExist

object Prism : Prism4j(object : GrammarLocator {
    val delegate = PrismBundleGrammarLocator()
    override fun grammar(prism4j: Prism4j, language: String) = when (language) {
        in extraGrammars -> extraGrammars[language]!!(prism4j)
        "qml" -> createQmlGrammar(prism4j)
        in listOf("html", "xml", "html", "mathml", "svg") -> createMarkupGrammar(prism4j)
        "kthtml" -> createKtHtmlGrammar(prism4j)
        "gdscript" -> createGdscriptGrammar()
        in arrayOf("nu", "nushell") -> createNushellGrammar()
        in arrayOf("cfg", "ini") -> createIniGrammar()
        else -> delegate.grammar(prism4j, language)
    }

    override fun languages() = setOf("qml", "kthtml") + delegate.languages()
}) {
    private val extraGrammars = mutableMapOf<String, (prism4j: Prism4j) -> Grammar>()

    fun registerGrammar(identifier: String, vararg extraIdentifiers: String, grammar: (prism4j: Prism4j) -> Grammar) {
        extraGrammars[identifier] = grammar
        for (extraIdentifier in extraIdentifiers) {
            extraGrammars[extraIdentifier] = grammar
        }
    }

    override fun tokenize(text: String, grammar: Grammar): MutableList<Node> {
        val entries: MutableList<Node> = ArrayList(3)
        entries.add(TextImpl(text)) // call the new matchGrammar instead of the bad one
        // try {
        matchGrammar(text, entries, grammar, 0, 0, false, null)
        // } catch (e: StackOverflowError) {
        //     err("Matching failed on\n${text.prependIndent("  ")}\n(stack overflow)")
        // }
        return entries
    }

    // copy-pasted because I needed to fix the textLength == 0 thing
    private fun matchGrammar(
        text: String,
        entries: MutableList<Node>,
        grammar: Grammar,
        index: Int,
        startPosition: Int,
        oneShot: Boolean,
        target: Token?
    ) {
        val textLength = text.length

        // the one real change
        if (textLength == 0) return

        for (token in grammar.tokens()) {
            if (token === target) return

            for (pattern in token.patterns()) {
                val lookbehind = pattern.lookbehind()
                val greedy = pattern.greedy()
                var lookbehindLength = 0

                val regex = pattern.regex()

                // Don't cache textLength as it changes during the loop
                var i = index
                var position = startPosition
                while (i < entries.size) {
                    if (entries.size > textLength) {
                        throw RuntimeException(
                            "Prism4j internal error. Number of entry nodes is greater that the text length.\nNodes: $entries\nText: $text"
                        )
                    }

                    val node = entries[i]
                    if (node.isSyntax) {
                        position += entries[i].textLength()
                        ++i
                        continue
                    }

                    var str = (node as Text).literal()

                    val matcher: Matcher
                    val deleteCount: Int
                    val greedyMatch: Boolean
                    var greedyAdd = 0

                    if (greedy && i != entries.size - 1) {
                        matcher = regex.matcher(text) // limit search to the position (?)
                        matcher.region(position, textLength)

                        if (!matcher.find()) {
                            break
                        }

                        var from = matcher.start()

                        if (lookbehind) {
                            from += matcher.group(1).length
                        }
                        val to = matcher.start() + matcher.group(0).length

                        var k = i
                        var p = position

                        val len = entries.size
                        while (k < len && (p < to || (!entries[k].isSyntax && entries[k - 1].isLazy))) {
                            p += entries[k].textLength() // Move the index i to the element in strarr that is closest to from
                            if (from >= p) {
                                i += 1
                                position = p
                            }
                            ++k
                        }

                        if (entries[i].isSyntax) {
                            position += entries[i].textLength()
                            ++i
                            continue
                        }

                        deleteCount = k - i
                        str = text.substring(position, p)
                        greedyMatch = true
                        greedyAdd = -position
                    } else {
                        matcher = regex.matcher(str)
                        deleteCount = 1
                        greedyMatch = false
                    }

                    if (!greedyMatch && !matcher.find()) {
                        if (oneShot) {
                            break
                        }
                        position += entries[i].textLength()
                        ++i
                        continue
                    }

                    if (lookbehind) lookbehindLength = matcher.group(1)?.length ?: 0

                    val from = matcher.start() + greedyAdd + lookbehindLength
                    val match =
                        if (lookbehindLength > 0) matcher.group().substring(lookbehindLength) else matcher.group()
                    val to = from + match.length

                    repeat(deleteCount) {
                        entries.removeAt(i)
                    }

                    var j = i

                    if (from != 0) {
                        val before = str.take(from)
                        i += 1
                        position += before.length
                        entries.add(j++, TextImpl(before))
                    }

                    val inside = pattern.inside()
                    val hasInside = inside != null
                    val tokenEntries = if (hasInside) tokenize(match, inside) else mutableListOf(TextImpl(match))

                    entries.add(j++, SyntaxImpl(token.name(), tokenEntries, pattern.alias(), match, greedy, hasInside))

                    // important thing here (famous off-by one error) to check against full length (not `length - 1`)
                    if (to < str.length) {
                        val after = str.substring(to)
                        entries.add(j, TextImpl(after))
                    }

                    if (deleteCount != 1) matchGrammar(text, entries, grammar, i, position, true, token)

                    if (oneShot) break
                    position += entries[i].textLength()
                    ++i
                }
            }
        }
    }

    private val Node.isLazy get() = !isSyntax || !(this as Syntax).greedy()
}

/**
 * @suppress
 */
fun generateNestedPattern(depth: Int = 4, opening: String = "{", closing: String = "}") =
    (1..depth).fold("[^$opening]*") { acc, _ -> "(?:[^$opening]|$opening$acc$closing)*" }