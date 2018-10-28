package com.github.jomof.buildserver.common

enum class Os(
        val tag : String,
        val exe : String,
        val bat : String,
        val classPathSeparator : String,
        val fileSeparator : String) {
    WINDOWS("windows", ".exe", ".bat", ";", "\\"),
    LINUX("linux", "", "", ":", "//"),
    DARWIN("darwin", "", "", ":", "//")
}

private val osName = System.getProperty("os.name")

val os = when {
    osName.startsWith("Win") -> Os.WINDOWS
    osName.startsWith("Mac") -> Os.DARWIN
    else -> Os.LINUX
}

