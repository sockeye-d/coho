package dev.fishies.coho.core

import dev.fishies.coho.core.ANSI.noColor
import dev.fishies.coho.core.ANSI.showVerbose

enum class Color(val code: Int) {
    Black(30),
    RED(31),
    Green(32),
    YELLOW(33),
    BLUE(34),
    MAGENTA(35),
    Cyan(36),
    WHITE(37),
    DEFAULT(39),
    RESET(0),
}

enum class Direction(val code: Char) {
    Up('A'),
    Down('B'),
    Right('C'),
    Left('D'),
};

object ANSI {
    var noColor: Boolean = System.getenv("NO_COLOR")?.isNotEmpty() ?: false
    var showVerbose: Boolean = false
}

const val ESC = "\u001b"
const val RESET = "$ESC[0m";
const val ERASE_LINE = "\r$ESC[0K";

fun fg(color: Color) = if (noColor) "" else "$ESC[${color.code}m"
fun bg(color: Color) = if (noColor) "" else "$ESC[${color.code + 10}m"

// u"%1[%2%3"_s.arg(esc).arg(count).arg(QString(static_cast<char>(direction)))
fun move(direction: Direction, distance: Int = 1) = "$ESC[$distance${direction.code}"

val ERROR = "${fg(Color.RED)}error:${RESET}"
val INFO = "${RESET}info:${RESET}"
val NOTE = "${fg(Color.YELLOW)}note:${RESET}"
val POSITIVE = "${fg(Color.Green)}success:${RESET}"

fun err(string: String, verbose: Boolean = false, lineEnding: String = "\n") =
    if (showVerbose || !verbose) System.err.print(string.prependIndent("$ERROR ") + lineEnding) else Unit

fun pos(string: String, verbose: Boolean = false, lineEnding: String = "\n") =
    if (showVerbose || !verbose) print(string.prependIndent("$POSITIVE ") + lineEnding) else Unit

fun note(string: String, verbose: Boolean = false, lineEnding: String = "\n") =
    if (showVerbose || !verbose) print(string.prependIndent("$NOTE ") + lineEnding) else Unit

fun info(string: String, verbose: Boolean = false, lineEnding: String = "\n") =
    if (showVerbose || !verbose) print(string.prependIndent("$INFO ") + lineEnding) else Unit