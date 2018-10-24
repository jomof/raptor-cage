package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.server.model.*
import java.io.File


private fun ClangCall.preprocessExtension() = if (isCcCompile) ".ii" else ".i"


/**
 * Convert this compile command to an equivalent command that just produces preprocessor
 * output (.i or .ii) format.
 */
fun ClangCall.toPreprocessEquivalent(preprocessFolder : File) : ClangCall {
    require(isCompile)
    val flags = flags
            .filter { flag ->
                !ClangFlagGroups.UNUSED_IN_PREPROCESS_ONLY_PHASE.contains(flag) }
            .map { flag ->
                when {
                    flag is OneArgFlag && flag.type == ClangFlagType.OUTPUT -> {
                        val output = File(preprocessFolder, flag.value + preprocessExtension())
                        listOf(flag.key, output.path)
                    }
                    else -> flag.sourceFlags
                }
            }
            .flatten()

    return ClangCall(flags + "-E")
}

fun ClangCall.redirectOutputs(cacheFolder : File) : ClangCall {
    val newFlags = flags
            .asSequence()
            .filter { flag -> !ClangFlagGroups.UNUSED_IN_POSTPROCESS_ONLY_PHASE.contains(flag) }
            .map {flag ->
                when {
                    flag is OneArgFlag && flag.type == ClangFlagType.OUTPUT-> {
                        listOf(flag.key, redirectUserFileToCacheFile(flag.value, cacheFolder))
                    }
                    else -> flag.sourceFlags
                }
            }
            .toList().flatten()
    return ClangCall(newFlags)
}

/**
 * Convert this compile command to an equivalent command that consumes the output of
 * {@link ClangCall.toPreprocessEquivalent}.
 */
fun ClangCall.toPostprocessEquivalent(preprocessFolder : File) : ClangCall {
    require(isCompile)
    val preprocessFile = File(preprocessFolder, lastOutput + preprocessExtension()).path
    val newFlags = flags
            .asSequence()
            .filter { flag -> !ClangFlagGroups.UNUSED_IN_POSTPROCESS_ONLY_PHASE.contains(flag) }
            .map {flag ->
                when (flag) {
                    is SourceFileFlag -> listOf(preprocessFile)
                    else -> flag.sourceFlags
                }
            }
            .toList().flatten()
    return ClangCall(newFlags)
}

fun redirectUserFileToCacheFile(userFile : String, cacheFolder : File) : String {
    require(!File(userFile).isRooted)
    val newFile = File(cacheFolder, userFile)
    return newFile.path
}

