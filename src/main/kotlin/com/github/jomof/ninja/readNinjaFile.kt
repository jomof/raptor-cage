package com.github.jomof.ninja

import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader

fun readNinjaFile(file : File) : NinjaFileDef {
    val topLevel = parseNinja(file)
    val includesExpanded = expandIncludes(topLevel) { include ->
        val included = File(file.parentFile, include)
        try {
            parseNinja(included)
        } catch (e : FileNotFoundException) {
            NinjaFileNotFound(included.path)
        }
    }
    val canonicalize = canonicalizeFiles(includesExpanded)

    return canonicalize
}