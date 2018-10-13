package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.common.flags.ClangFlags
import com.github.jomof.buildserver.common.io.RemoteStdio
import com.github.jomof.buildserver.common.process.redirectAndWaitFor
import java.io.File
import java.io.ObjectOutputStream

fun clang(
        directory: String,
        args: List<String>,
        write: ObjectOutputStream) : Int {
    val stdio = RemoteStdio(write)
    val flags = ClangFlags(args.toList())
    if (flags.operation.isObjectOutput()) {
        val preprocess = flags.toPreprocessEquivalent()
        val postproces = flags.toPostprocessEquivalent()
        stdio.stdout("Raptor cage writing ${preprocess.lastOutput}")
        val preprocessCode = execute(directory, preprocess.rawFlags.toTypedArray(), stdio)
        if (preprocessCode != 0) {
            stdio.exit()
            return preprocessCode
        }
        stdio.stdout("Raptor cage writing ${postproces.lastOutput}")
        val code = execute(directory, postproces.rawFlags.toTypedArray(), stdio)
        stdio.exit()
        return code
    }
    val code = execute(directory, args.toTypedArray(), stdio)
    stdio.exit()
    return code
}

private fun execute(
        directory: String,
        args: Array<String>,
        stdio: RemoteStdio) : Int {
    return ProcessBuilder(args.toList())
            .directory(File(directory))
            .start()
            .redirectAndWaitFor(stdio)
}