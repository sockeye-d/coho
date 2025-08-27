package dev.fishies.coho.core

import dev.fishies.coho.core.ANSI.noColor

enum class Color(val code: Int) {
    Black(30),
    Red(31),
    Green(32),
    Yellow(33),
    Blue(34),
    Magenta(35),
    Cyan(36),
    White(37),
    Default(39),
    Reset(0),
}

object ANSI {
    var noColor: Boolean = false
}

const val ESC = "\u001b"
const val RESET = "$ESC[0m";

fun fg(color: Color) = if (noColor) "" else "$ESC[${color.code}m"
fun bg(color: Color) = if (noColor) "" else "$ESC[${color.code + 10}m"

val ERROR = "${fg(Color.Red)}error:$RESET"

val INFO = "${RESET}info:$RESET"

val NOTE = "${fg(Color.Yellow)}note:$RESET"

val POSITIVE = "${fg(Color.Green)}success:$RESET"


fun err(string: String) = System.err.println(string.prependIndent("$ERROR "))
fun pos(string: String) = println(string.prependIndent("$POSITIVE "))
fun note(string: String) = println(string.prependIndent("$NOTE "))
fun info(string: String) = println(string.prependIndent("$INFO "))