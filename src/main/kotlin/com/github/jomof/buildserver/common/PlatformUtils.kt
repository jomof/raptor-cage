package com.github.jomof.buildserver.common

fun isWindows(): Boolean {
    return System.getProperty("os.name").startsWith("Win")
}

fun platformQuote(file: String): String {
    return if (isWindows()) {
        "\"" + file + "\""
    } else file
}