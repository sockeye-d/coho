package dev.fishies.coho.core

enum class TerminalColor(val code: Int) {
    Black(30), Red(31), Green(32), Yellow(33), Blue(34), Magenta(35), Cyan(36), White(37), Default(39)
}

enum class CursorDirection(val code: Char) {
    Up('A'), Down('B'), Right('C'), Left('D'),
};

object Ansi {
    /**
     * Whether to show colorized output.
     */
    var noColor: Boolean = System.getenv("NO_COLOR")?.isNotEmpty() ?: false

    /**
     * Whether to show [err]/[pos]/[note]/[info] messages with `verbose = true`.
     */
    var showVerbose: Boolean = false
}

/**
 * The ANSI escape sequence escape.
 */
const val esc = "\u001b"

/**
 * Reset all text effects.
 */
const val reset = "$esc[0m";

const val bold = "$esc[1m"
const val faint = "$esc[2m"
const val italic = "$esc[3m"

/**
 * Set the foreground color to [color].
 * @param color The color to change to.
 * @return An ANSI escape sequence
 */
fun fg(color: TerminalColor) = if (Ansi.noColor) "" else "$esc[${color.code}m"

/**
 * Set the background color to [color].
 * @param color The color to change to.
 * @return An ANSI escape sequence
 */
fun bg(color: TerminalColor) = if (Ansi.noColor) "" else "$esc[${color.code + 10}m"

/**
 * Move the terminal curser in [distance] cells in [direction].
 * @return An ANSI escape sequence
 */
fun move(direction: CursorDirection, distance: Int = 1) = "$esc[$distance${direction.code}"

/**
 * Print [string] prefixed with `error:` to stderr with the [lineEnding].
 * @param verbose Whether the message should only show up if [Ansi.showVerbose] is true.
 */
fun err(string: String, verbose: Boolean = false, lineEnding: String = "\n") =
    if (Ansi.showVerbose || !verbose) System.err.print(string.prependIndent("${fg(TerminalColor.Red)}error:${reset} ") + lineEnding) else Unit

/**
 * Print [string] prefixed with `success:` to stdout with the [lineEnding].
 * @param verbose Whether the message should only show up if [Ansi.showVerbose] is true.
 */
fun pos(string: String, verbose: Boolean = false, lineEnding: String = "\n") =
    if (Ansi.showVerbose || !verbose) print(string.prependIndent("${fg(TerminalColor.Green)}success:${reset} ") + lineEnding) else Unit

/**
 * Print [string] prefixed with `note:` to stdout with the [lineEnding].
 * @param verbose Whether the message should only show up if [Ansi.showVerbose] is true.
 */
fun note(string: String, verbose: Boolean = false, lineEnding: String = "\n") =
    if (Ansi.showVerbose || !verbose) print(string.prependIndent("${fg(TerminalColor.Yellow)}note:${reset} ") + lineEnding) else Unit

/**
 * Print [string] prefixed with `info:` to stdout with the [lineEnding].
 * @param verbose Whether the message should only show up if [Ansi.showVerbose] is true.
 */
fun info(string: String, verbose: Boolean = false, lineEnding: String = "\n") =
    if (Ansi.showVerbose || !verbose) print(string.prependIndent("${reset}info:${reset} ") + lineEnding) else Unit