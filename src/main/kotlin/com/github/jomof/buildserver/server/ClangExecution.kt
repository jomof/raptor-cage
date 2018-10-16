package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.common.flags.ClangFlags
import com.github.jomof.buildserver.common.flags.OneArgFlag
import com.github.jomof.buildserver.common.flags.SourceFileFlag
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
    val flags = ClangFlags(args.toList())

    fun execute(args: Array<String>) : Int {
        return ProcessBuilder(args.toList())
                .directory(File(directory))
                .start()
                .redirectAndWaitFor(stdio)
    }

    try {
        if (flags.operation.isObjectOutput()) {
            val joined = flags.joinToString()
            val storeHandle = StoreHandle(serverName,"compile-flags", joined)
            val readableStore = storeHandle.readable()
            if (readableStore == null) {
                // If readable is null then these flags haven't been seen before.
                val writeableStore = storeHandle.writeable()
                val preprocess = flags.toPreprocessEquivalent(writeableStore)
                stdio.stdout("Raptor cage writing ${preprocess.lastOutput}")
                val preprocessCode = execute(preprocess.rawFlags.toTypedArray())
                if (preprocessCode != 0) {
                    return preprocessCode
                }

                val postproces = flags
                        .redirectOutputs(writeableStore)
                        .toPostprocessEquivalent(writeableStore)
                stdio.stdout("Raptor cage writing ${postproces.lastOutput}")
                val result = execute(postproces.rawFlags.toTypedArray())
                storeHandle.commit()
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


private val unusedInPostProcessPhase = setOf("MD", "MT", "isystem", "MF")

private fun ClangFlags.preprocessExtension() = if (isCcCompile) ".ii" else ".i"

fun ClangFlags.toPreprocessEquivalent(preprocessFolder : File? = null) : ClangFlags {
    require(isCompile)
    val flags = flags.map { flag ->
        when {
            flag is OneArgFlag && flag.isFlag("o") -> {
                if (preprocessFolder == null) {
                    listOf(flag.key, flag.value + preprocessExtension())
                } else {
                    val output = File(preprocessFolder, File(flag.value).name + preprocessExtension())
                    listOf(flag.key, output.path)
                }
            }
            else -> flag.sourceFlags
        }
    }.flatten()
            .filter {
                when(it) {
                    // Running just the preprocessor doesn't use this flag, so remove
                    "-Wa,--noexecstack" -> false
                    else -> true
                }

            }
    return ClangFlags(flags + "-E")
}

fun ClangFlags.redirectOutputs(cacheFolder : File) : ClangFlags {
    val newFlags = flags
            .asSequence()
            .filter { flag -> !unusedInPostProcessPhase.any { flag.isFlag(it) } }
            .map {flag ->
                when {
                    flag is OneArgFlag && flag.isFlag("o") -> {
                        listOf(flag.key, redirectUserFileToCacheFile(flag.value, cacheFolder))
                    }
                    else -> flag.sourceFlags
                }
            }
            .toList().flatten()
    return ClangFlags(newFlags)

}

fun ClangFlags.toPostprocessEquivalent(preprocessFolder : File? = null) : ClangFlags {
    require(isCompile)
    val preprocessFile = if (preprocessFolder == null) {
        lastOutput + preprocessExtension()
    } else {
        File(preprocessFolder, File(lastOutput).name + preprocessExtension()).path
    }
    val newFlags = flags
            .asSequence()
            .filter { flag -> !unusedInPostProcessPhase.any { flag.isFlag(it) } }
            .map {flag ->
                when {
                    flag is SourceFileFlag -> listOf(preprocessFile)
                    else -> flag.sourceFlags
                }
            }
            .toList().flatten()
    return ClangFlags(newFlags)
}

fun redirectUserFileToCacheFile(userFile : String, cacheFolder : File) : String {
    require(!File(userFile).isRooted)
    val newFile = File(cacheFolder, userFile)
    return newFile.path
}

