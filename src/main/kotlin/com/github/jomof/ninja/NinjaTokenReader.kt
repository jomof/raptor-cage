package com.github.jomof.ninja

import java.io.Reader

const val END_OF_LINE_TOKEN = "::END_OF_LINE_TOKEN::"
const val END_OF_FILE_TOKEN = "::END_OF_FILE_TOKEN::"
const val INDENT_TOKEN = "::INDENT_TOKEN::"

fun Reader.forEachNonComment(action: (String) -> Unit) {
    forEachLine { line ->
        if(line.substringBefore("#").isNotBlank()) {
            action(line.trimEnd())
        } else {
            action("")
        }
    }
}

fun Reader.forEachContinuedLine(action: (String) -> Unit) {
    var continued = ""
    fun continuation() {
        if (!continued.isEmpty()) {
            action(continued)
            continued = ""
        }
    }
    forEachNonComment { line ->
        val trimmed =
                if (!continued.isEmpty()) {
                    line.trimStart()
                } else {
                    line
                }
        if (line.endsWith("$")) {
            continued += trimmed.substringBeforeLast("$")
        } else {
            continued += trimmed
            continuation()
        }
    }
    continuation()
}

private fun escape(line: String) : String {
    val l1 = line.replace("\$\$", "[\$dollar\$]")
    val l2 = l1.replace("$:", "[\$colon\$]")
    val l3 = l2.replace("$ ", "[\$space\$]")
    return l3
}

private fun unescape(line: String) = line
        .replace("[\$dollar\$]", "$")
        .replace("[\$colon\$]", ":")
        .replace("[\$space\$]", " ")

private fun tokenizeAround(separatorToken: Char, value: String, action: (String) -> Unit) {
    if (value.isEmpty()) return
    if (!value.contains(separatorToken)) {
        action(value)
        return
    }
    if (value.trim(separatorToken).isEmpty()) {
        action(value)
        return
    }
    val sb = StringBuilder()
    for (c in value) {
        if (c == separatorToken) {
            if (!sb.isEmpty()) {
                action(unescape(sb.toString()))
                sb.setLength(0)
            }
            action(separatorToken.toString())
        } else {
            sb.append(c)
        }
    }
    if (!sb.isEmpty()) {
        action(unescape(sb.toString()))
    }
}

fun Reader.forEachNinjaToken(action: (String) -> Unit) {
    forEachContinuedLine { line ->
        if (line.contains("=")) {
            if (line.startsWith(" ")) {
                action(INDENT_TOKEN)
            }

            action(line.substringBefore('=').trim())
            action("=")
            val remainder = line.substringAfter('=').trim()
            val escaped = escape(remainder)
            val unescaped = unescape(escaped)
            action(unescaped)
        } else {
            val escaped = escape(line.substringBefore("#"))
            val spaceSplits = escaped.split(' ')
            for (spaceSplit in spaceSplits) {
                tokenizeAround(':', spaceSplit) { colonSplit ->
                    tokenizeAround('|', colonSplit) { pipeSplit ->
                        tokenizeAround('=', pipeSplit) { equalSplit ->
                            action(unescape(equalSplit))
                        }
                    }
                }
            }
        }
        action(END_OF_LINE_TOKEN)
    }
    action(END_OF_FILE_TOKEN)
}