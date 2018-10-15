package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.common.flags.ClangFlags
import com.github.jomof.buildserver.common.io.RemoteStdio
import com.github.jomof.buildserver.common.process.redirectAndWaitFor
import java.io.File
import java.io.ObjectOutputStream

/**
 * Handles calls to clang on the server.
 */
fun clang(
        directory: String,
        args: List<String>,
        write: ObjectOutputStream) : Int {
    val stdio = RemoteStdio(write)
    val flags = ClangFlags(args.toList())

    fun execute(args: Array<String>) : Int {
        return ProcessBuilder(args.toList())
                .directory(File(directory))
                .start()
                .redirectAndWaitFor(stdio)
    }

    try {
        if (flags.operation.isObjectOutput()) {
            val flagsFile = File(directory, flags.outputs.single() + ".flags")
            flagsFile.parentFile.mkdirs()
            stdio.stdout("Raptor cage writing $flagsFile")
            flagsFile.writeText(args.joinToString("\n"))

            val preprocess = flags.toPreprocessEquivalent()
            val postproces = flags.toPostprocessEquivalent()
            stdio.stdout("Raptor cage writing ${preprocess.lastOutput}")
            val preprocessCode = execute(preprocess.rawFlags.toTypedArray())
            if (preprocessCode != 0) {
                return preprocessCode
            }
            stdio.stdout("Raptor cage writing ${postproces.lastOutput}")
            return execute(postproces.rawFlags.toTypedArray())
        }
        return execute(args.toTypedArray())
    } finally {
        stdio.exit()
    }
}
