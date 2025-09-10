package dev.fishies.coho.core.highlighting

import io.noties.prism4j.GrammarUtils
import io.noties.prism4j.Prism4j
import io.noties.prism4j.Prism4j.*
import java.util.regex.Pattern.DOTALL
import java.util.regex.Pattern.MULTILINE
import java.util.regex.Pattern.compile

// language=regexp
private const val IDENTIFIER = "[a-zA-Z_][a-zA-Z0-9-_]*"

// language=regexp
private const val QUALIFIED_IDENTIFIER = "($IDENTIFIER\\.)*$IDENTIFIER"

private const val TYPED_IDENTIFIER = "$IDENTIFIER\\s*"
private const val TYPED_ARGUMENT_LIST = "($TYPED_IDENTIFIER?)|($TYPED_IDENTIFIER\\s*,\\s*)"

// language=regexp
private const val BUILTINS =
    "debug experimental-options|str screaming-snake-case|help pipe-and-redirect|commandline set-cursor|commandline get-cursor|keybindings default|scope engine-stats|keybindings listen|date list-timezone|config use-colors|bytes starts-with|attr search-terms|path relative-to|keybindings list|encode base32hex|decode base32hex|date to-timezone|commandline edit|url split-query|url build-query|str starts-with|str pascal-case|split cell-path|scope variables|metadata access|history session|format filesize|format duration|date from-human|bytes ends-with|attr deprecated|str title-case|str snake-case|str kebab-case|str capitalize|str camel-case|scope commands|into cell-path|history import|help operators|format pattern|detect columns|config flatten|bytes index-of|version check|str substring|str ends-with|scope modules|scope externs|scope aliases|random binary|path basename|math variance|into filesize|into duration|into datetime|help commands|from msgpackz|format number|encode base64|encode base32|decode base64|decode base32|debug profile|date humanize|bytes reverse|bytes replace|bytes collect|attr category|ansi gradient|update cells|str index-of|str downcase|str distance|str contains|split column|run-external|random float|random chars|path dirname|overlay list|nu-highlight|metadata set|math product|math arctanh|math arcsinh|math arccosh|job unfreeze|is-not-empty|input listen|http options|help modules|help externs|help escapes|help aliases|from msgpack|config reset|bytes remove|bytes length|attr example|view source|view blocks|to msgpackz|str reverse|str replace|stor update|stor insert|stor import|stor export|stor delete|stor create|split words|split chars|random uuid|random dice|random bool|plugin stop|plugin list|path expand|path exists|math stddev|math median|math arctan|math arcsin|math arccos|keybindings|is-terminal|into string|into sqlite|into record|into binary|http delete|hash sha256|format date|format bits|drop column|date format|commandline|bytes split|bytes build|view files|url encode|url decode|to msgpack|term query|take while|take until|str upcase|str length|str expand|stor reset|split list|skip while|skip until|roll right|random int|plugin add|path split|path parse|merge deep|math round|math floor|into value|into float|interleave|input list|http patch|error make|encode hex|each while|decode hex|debug info|config env|ansi strip|view span|url parse|transpose|term size|sys users|sys disks|str stats|stor open|split row|roll left|roll down|plugin rm|path type|path self|path join|math tanh|math sqrt|math sinh|math mode|math cosh|math ceil|job spawn|job flush|into glob|into bool|http post|http head|histogram|from yaml|from xlsx|from toml|from nuon|from json|enumerate|debug env|config nu|bytes add|ansi link|with-env|url join|sys temp|sys host|str trim|str join|seq date|seq char|query db|par-each|nu-check|metadata|math tan|math sum|math sin|math min|math max|math log|math exp|math cos|math avg|math abs|load-env|job send|job recv|job list|job kill|is-empty|is-admin|into int|http put|http get|hide-env|hash md5|group-by|generate|from yml|from xml|from url|from tsv|from ssv|from ods|from csv|drop nth|describe|date now|complete|chunk-by|bytes at|bits xor|bits shr|bits shl|bits ror|bits rol|bits not|bits and|view ir|version|uniq-by|to yaml|to toml|to text|to nuon|to json|to html|sys net|sys mem|sys cpu|sort-by|shuffle|roll up|reverse|prepend|math ln|let-env|job tag|inspect|history|headers|flatten|explore|explain|default|compact|columns|collect|bits or|window|whoami|values|upsert|update|ulimit|to yml|to xml|to tsv|to csv|timeit|select|schema|rotate|rename|reject|reduce|random|plugin|mktemp|length|job id|insert|ignore|format|filter|encode|decode|config|chunks|append|which|watch|uname|tutor|touch|to md|table|start|split|slice|sleep|scope|print|parse|panic|mkdir|merge|lines|items|input|first|every|debug|clear|bytes|wrap|view|uniq|term|take|stor|sort|skip|save|roll|port|path|open|move|math|last|kill|join|into|http|help|hash|grid|glob|from|find|fill|exit|exec|echo|each|drop|date|char|bits|ansi|zip|url|tee|sys|str|seq|job|get|cal|ast|any|all|to|rm|ps|mv|ls|du|do|cp|cd"

fun createNushellGrammar(prism: Prism4j): Grammar {
    val argumentList = grammar(
        "argument-list",
        token(
            "argument",
            pattern(compile("(?<=,\\s*(?:\\.\\s*\\.\\s*\\.\\s*)?)$IDENTIFIER")),
            pattern(compile("(?<=^\\s*)$IDENTIFIER"))
        ),
        token("declaration-type", pattern(compile("(?<=:\\s*)$IDENTIFIER"), false, false, "class-name")),
        token("punctuation", pattern(compile("[,:.=]"))),
    )
    val nu = grammar(
        "nu",
        token("comment", pattern(compile("#.*\\n"))),
        token("single-quote-string", pattern(compile("'.*?'"), false, true, "string")),
        token("interpolated-string"),
        token("double-quote-string", pattern(compile("\"[^\"]*?\""), false, true, "string")),
        token("raw-string", pattern(compile("r(#+)'.*?'\\1"), false, true, "string")),
        token("variable", pattern(compile("\\$$IDENTIFIER(.$IDENTIFIER)*"))),
        token(
            "number",
            pattern(compile("\\d+(\\.\\d+)?")),
            pattern(compile("0x[0-9a-fA-F]+")),
            pattern(compile("0o[0-7]+")),
            pattern(compile("0b[0-1]+")),
        ),
        token(
            "variable-declaration", pattern(compile("(?<=(?:let|mut)\\s+)$IDENTIFIER"), false, false, "variable")
        ),
        token(
            "argument-declaration",
            pattern(compile("(?<=\\{\\s*\\|).*?(?=\\|)"), false, true, null, argumentList),
            pattern(compile("(?<=def\\s+$IDENTIFIER\\s+\\[).*?(?=]\\s+\\{)"), false, true, null, argumentList),
            pattern(compile("$IDENTIFIER(?=\\s*:)"), false, true, "argument"),
        ),
        token(
            "declaration-type", pattern(compile("(?<=:\\s*)$IDENTIFIER"), false, false, "class-name")
        ),
        token(
            "argument", pattern(compile("(?<=^|\\s)-+[\\w-]+(?=$|\\s)"))
        ),
        token(
            "path", pattern(
                compile("(\\.\\/|..\\/|\\/|\\.\\\\[^\\s(){}\\[\\]&|;]*|\\\\\\\\\\?\\\\)[^\\s(){}\\[\\]&|;]*|~(\\/[^\\s(){}\\[\\]&|;]*)?"),
                false,
                false,
                "url"
            )
        ),
        token(
            "if-command",
            pattern(compile("(?<=if\\s*\\(?)$IDENTIFIER"), false, false, "function")
        ),
        token(
            "keyword",
            pattern(compile("\\b(?:export module|export extern|overlay hide|export const|export alias|overlay use|overlay new|export use|export def|plugin use|source-env|export-env|continue|overlay|export|source|return|module|extern|where|break|while|const|alias|match|false|null|hide|loop|else|true|mut|let|for|use|def|try|if)\\b"))
        ),
        token(
            "builtin-function", pattern(compile(BUILTINS), false, false, "function")
        ),
        token(
            "command",
            pattern(compile("(?<=^\\s*)[\\w-]+", MULTILINE), false, false, "function"),
            pattern(compile("(?<=\\(\\s*)[\\w-]+", MULTILINE), false, false, "function"),
        ),
        token(
            "punctuation", pattern(compile("[|{}:\\[\\],]"))
        ),
    )

    with(GrammarUtils.findToken(nu, "interpolated-string") ?: error("Interpolated string token not found")) {
        val innerPattern = pattern(
            compile("""(?<=(?<!\\)(?:\\\\){0,100}\().*?(?=(?<!\\)(?:\\\\){0,100}\))"""), false, true, null, nu
        )
        val leftBracketInnerPattern = pattern(
            compile("""(?<=(?<!\\)(?:\\\\){0,100})\)"""), false, true
        )
        val rightBracketInnerPattern = pattern(
            compile("""(?<=(?<!\\)(?:\\\\){0,100})\((?=.*?(?<!\\)(?:\\\\){0,100}\))"""), false, true
        )
        patterns().addAll(
            listOf(
                pattern(
                    compile("\\$\".*?\""), false, true, "string", grammar(
                        "inside",
                        token("punctuation", innerPattern),
                        token("delimiter", leftBracketInnerPattern),
                        token("delimiter", rightBracketInnerPattern),
                        token("string", pattern(compile(".*")))
                    )
                )
            )
        )
    }

    return nu
}