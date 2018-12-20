package com.github.jomof.ninja

import java.io.File
import java.io.FileReader


fun readNinjaFile(file : File) : NinjaFileDef {
    val topLevel = parseNinja(file.absolutePath, FileReader(file))
    val includesExpanded = expandIncludes(topLevel) { include ->
        parseNinja(file.absolutePath, FileReader(File(file.parentFile, include)))
    }
    val canonicalize = canonicalizeFiles(includesExpanded)

    return canonicalize
}