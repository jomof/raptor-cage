package com.github.jomof.buildserver.ninja

import java.io.File
import java.io.FileReader


fun readNinjaFile(file : File) : NinjaFileDef {
    val topLevel = parseNinja(FileReader(file))
    val includesExpanded = expandIncludes(topLevel) { include ->
        parseNinja(FileReader(File(file.parentFile, include)))
    }
    val canonicalize = canonicalizeFiles(includesExpanded)

    return canonicalize
}