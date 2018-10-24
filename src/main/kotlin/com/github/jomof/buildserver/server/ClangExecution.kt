package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.server.model.ClangCall
import com.github.jomof.buildserver.common.io.RemoteStdio
import com.github.jomof.buildserver.common.process.redirectAndWaitFor
import com.github.jomof.buildserver.server.store.StoreHandle
import java.io.File
import java.io.ObjectOutputStream

/**
 * Handles calls to clang on the server.
 */
fun clang(
        serverName: String,
        directory: String,
        args: List<String>,
        write: ObjectOutputStream) : Int {
    val stdio = RemoteStdio(write)
    val flags = ClangCall(args.toList())

    fun execute(args: Array<String>) : Int {
        return ProcessBuilder(args.toList())
                .directory(File(directory))
                .start()
                .redirectAndWaitFor(stdio)
    }

    try {
        if (flags.operation.isObjectOutput()) {
            val joined = flags.joinToString()
            val storeHandle = StoreHandle(serverName, "compile-flags", joined)
            val readableStore = storeHandle.readable()
            if (readableStore == null) {
                // If readable is null then these flags haven't been seen before.
                val writeableStore = storeHandle.writeable()
                val preprocess = flags
                        .toPreprocessEquivalent(writeableStore)
                stdio.stdout("Raptor cage writing ${preprocess.lastOutput}")
                File(preprocess.lastOutput).parentFile.mkdirs()
                val preprocessCode = execute(preprocess.rawFlags.toTypedArray())
                if (preprocessCode != 0) {
                    return preprocessCode
                }
                val postprocess = flags
                        .toPostprocessEquivalent(writeableStore)
                        .redirectOutputs(writeableStore)
                stdio.stdout("Raptor cage writing ${postprocess.lastOutput}")
                val result = execute(postprocess.rawFlags.toTypedArray())
                storeHandle.commit()

                val finalOutput = File(directory, flags.lastOutput)
                stdio.stdout("Raptor cage writing to $finalOutput")
                File(postprocess.lastOutput).copyTo(finalOutput)
                return result
            }
            stdio.stdout("Raptor cage found cache")
            return execute(args.toTypedArray())
        }
        return execute(args.toTypedArray())
    } finally {
        stdio.exit()
    }
}

