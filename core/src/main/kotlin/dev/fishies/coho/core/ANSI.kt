package dev.fishies.coho.core

enum class TerminalColor(val code: Int) {
    BLACK(30), RED(31), GREEN(32), YELLOW(33), BLUE(34), MAGENTA(35), CYAN(36), WHITE(37), DEFAULT(39), RESET(0),
}

enum class CursorDirection(val code: Char) {
    UP('A'), DOWN('B'), RIGHT('C'), LEFT('D'),
};

object ANSI {
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
const val ESC = "\u001b"

/**
 * Reset all text effects.
 */
const val RESET = "$ESC[0m";

const val BOLD = "$ESC[1m"
const val FAINT = "$ESC[2m"
const val ITALIC = "$ESC[3m"
const val UNDERLINE = "$ESC[4m"
const val BLINKING = "$ESC[5m"
const val INVERT = "$ESC[6m"
const val INVISIBLE = "$ESC[7m"
const val STRIKETHROUGH = "$ESC[8m"

/**
 * Set the foreground color to [color].
 * @param color The color to change to.
 * @return An ANSI escape sequence
 */
fun fg(color: TerminalColor) = if (ANSI.noColor) "" else "$ESC[${color.code}m"

/**
 * Set the background color to [color].
 * @param color The color to change to.
 * @return An ANSI escape sequence
 */
fun bg(color: TerminalColor) = if (ANSI.noColor) "" else "$ESC[${color.code + 10}m"

/**
 * Move the terminal curser in [distance] cells in [direction].
 * @return An ANSI escape sequence
 */
fun move(direction: CursorDirection, distance: Int = 1) = "$ESC[$distance${direction.code}"

/**
 * Prefix for error messages (red `error:`)
 */
val ERROR = "${fg(TerminalColor.RED)}error:${RESET}"

/**
 * Prefix for info messages (`info:`)
 */
val INFO = "${RESET}info:${RESET}"

/**
 * Prefix for error messages (yellow `note:`).
 */
val NOTE = "${fg(TerminalColor.YELLOW)}note:${RESET}"

/**
 * Prefix for success messages (green `success:`)
 */
val POSITIVE = "${fg(TerminalColor.GREEN)}success:${RESET}"

/**
 * Print [string] prefixed with [ERROR] to stderr with the [lineEnding].
 * @param verbose Whether the message should only show up if [ANSI.showVerbose] is true.
 */
fun err(string: String, verbose: Boolean = false, lineEnding: String = "\n") =
    if (ANSI.showVerbose || !verbose) System.err.print(string.prependIndent("$ERROR ") + lineEnding) else Unit

/**
 * Print [string] prefixed with [POSITIVE] to stdout with the [lineEnding].
 * @param verbose Whether the message should only show up if [ANSI.showVerbose] is true.
 */
fun pos(string: String, verbose: Boolean = false, lineEnding: String = "\n") =
    if (ANSI.showVerbose || !verbose) print(string.prependIndent("$POSITIVE ") + lineEnding) else Unit

/**
 * Print [string] prefixed with [NOTE] to stdout with the [lineEnding].
 * @param verbose Whether the message should only show up if [ANSI.showVerbose] is true.
 */
fun note(string: String, verbose: Boolean = false, lineEnding: String = "\n") =
    if (ANSI.showVerbose || !verbose) print(string.prependIndent("$NOTE ") + lineEnding) else Unit

/**
 * Print [string] prefixed with [INFO] to stdout with the [lineEnding].
 * @param verbose Whether the message should only show up if [ANSI.showVerbose] is true.
 */
fun info(string: String, verbose: Boolean = false, lineEnding: String = "\n") =
    if (ANSI.showVerbose || !verbose) print(string.prependIndent("$INFO ") + lineEnding) else Unit