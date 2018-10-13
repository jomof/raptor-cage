package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.common.flags.ClangFlags
import java.io.File
import java.io.ObjectOutputStream

fun clang(
        directory : String,
        args : List<String>, write : ObjectOutputStream) : Int {
    val flags = ClangFlags(args.toList())
    if (flags.operation.isObjectOutput()) {
        val preprocess = flags.toPreprocessEquivalent()
        val postproces = flags.toPostprocessEquivalent()
        println("Raptor cage writing ${preprocess.lastOutput}")
        execute(directory, preprocess.rawFlags.toTypedArray())
        println("Raptor cage writing ${postproces.lastOutput}")
        return execute(directory, postproces.rawFlags.toTypedArray())
    }
    return execute(directory, args.toTypedArray())
}

private fun execute(directory : String, args: Array<String>): Int {
    val process = ProcessBuilder(args.toList())
            .directory(File(directory))
            .inheritIO()
            .start()
//            val outputStream = process.outputStream
//            val errorStream = process.errorStream

    return process.waitFor()
}