package com.github.jomof.buildserver.server.model

import com.github.jomof.buildserver.server.model.ClangOperation.*
import com.github.jomof.buildserver.server.model.ClangFlagGroups.*

data class ClangCall(val rawFlags : List<String>) {
    val flags = interpretFlags(rawFlags)
    val fileExtensions = flags
            .map { flag -> flag.flag.substringAfterLast(".", "") }
            .filter { extension -> knownFileExtensions.contains(extension) }
            .toSet()
    val sourceFiles = flags.mapNotNull {
        when(it) {
            is SourceFileFlag -> it.flag
            else -> null
        }
    }
    val lastSourceFile = sourceFiles.lastOrNull()
    val isPreprocessorRun = rawFlags.contains("-E")
    private val cFileExtensions = (fileExtensions intersect knownCFileExtensions)
    private val ccFileExtensions = (fileExtensions intersect knownCcFileExtensions)
    private val iFileExtensions = (fileExtensions intersect knownIFileExtensions)
    private val iiFileExtensions = (fileExtensions intersect knownIiFileExtensions)
    private val objectFileExtensions = (fileExtensions intersect knownObjectFileExtensions)
    private val isObjectOutput = !objectFileExtensions.isEmpty()
    val isCcCompile = !ccFileExtensions.isEmpty() && !objectFileExtensions.isEmpty()
    val isCCompile = !cFileExtensions.isEmpty() && !objectFileExtensions.isEmpty()
    private val isIiCompile = !iiFileExtensions.isEmpty() && !objectFileExtensions.isEmpty()
    private val isICompile = !iFileExtensions.isEmpty() && !objectFileExtensions.isEmpty()
    val isCompile = isCCompile || isCcCompile || isIiCompile || isICompile
    private val outputFlags = flags
            .filterIsInstance<OneArgFlag>()
            .filter { it.key == "-o" || it.key == "-o" }
    private val outputs = outputFlags.map { it.value }
    val lastOutput = outputs.lastOrNull()

    val operation = when {
        !isPreprocessorRun && isICompile && isObjectOutput -> I_TO_O
        !isPreprocessorRun && isIiCompile && isObjectOutput -> II_TO_O
        !isPreprocessorRun && isCCompile && isObjectOutput -> C_TO_O
        !isPreprocessorRun && isCcCompile && isObjectOutput -> CC_TO_O
        isPreprocessorRun && isCCompile && isObjectOutput -> C_TO_I
        isPreprocessorRun && isCcCompile && isObjectOutput -> CC_TO_II
        else -> UNKNOWN
    }

    private fun interpretFlags(flags : List<String>) : List<ClangFlag> {
        val result = mutableListOf<ClangFlag>()
        var i = 0
        while (i < flags.size) {
            val flag = flags[i]
            when {
                flags.size != i + 1 && ONE_ARG_CLANG_FLAGS.contains(flag) -> {
                    result.add(OneArgFlag(
                            flag,
                            flags[i + 1],
                            listOf(flag, flags[i + 1]),
                            ONE_ARG_CLANG_FLAGS.typeOf(flag)
                            ))
                    ++i
                }
                ONE_ARG_CLANG_FLAGS.canSplit(flag) -> {
                    val (key,value) = ONE_ARG_CLANG_FLAGS.split(flag)!!
                    result.add(OneArgFlag(
                            key,
                            value,
                            listOf(flag),
                            ONE_ARG_CLANG_FLAGS.typeOf(key)))
                }
                PASS_THROUGH_CLANG_FLAGS.canSplit(flag) -> {
                    val (key,value) = PASS_THROUGH_CLANG_FLAGS.split(flag)!!
                    result.add(PassThroughFlag(
                            key,
                            value,
                            listOf(flag),
                            PASS_THROUGH_CLANG_FLAGS.typeOf(key)))
                }
                !flag.startsWith("-")
                        && knownSourceFileExtensions.any { flag.endsWith(".$it") } ->
                    result.add(SourceFileFlag(flag))
                else -> result.add(UnidentifiedClangFlag(flag))
            }
            ++i
        }
        return result
    }

    fun withClangExecutable(executable : String) : ClangCall {
        return ClangCall(listOf(executable) + rawFlags.drop(1))
    }

    fun withSourceInput(source : String) : ClangCall {
        require(isCompile)
        val newFlags = flags
                .map {
                    when(it) {
                        is SourceFileFlag ->
                            listOf(source)
                        else -> it.sourceFlags
                    }
                }.flatten()
        val result = ClangCall(newFlags)
        require(result.isCompile)
        return result
    }

    fun withOutput(output : String) : ClangCall {
        require(isCompile)
        val flags = flags.map { flag ->
            when {
                flag is OneArgFlag && flag.type == ClangFlagType.OUTPUT ->
                    listOf(flag.key, output)
                else -> flag.sourceFlags
            }
        }.flatten()
        return ClangCall(flags)
    }

    fun joinToString() : String {
        return rawFlags.joinToString("\n")
    }

    companion object {
        private val knownCFileExtensions = setOf("c")
        private val knownCcFileExtensions = setOf("c++", "cpp", "cc")
        private val knownIFileExtensions = setOf("i")
        private val knownIiFileExtensions = setOf("ii")
        private val knownPostProcessFileExtensions = knownIFileExtensions + knownIiFileExtensions
        private val knownSourceFileExtensions = knownCFileExtensions + knownCcFileExtensions + knownPostProcessFileExtensions
        private val knownObjectFileExtensions = setOf("o")
        val knownFileExtensions =
                setOf("exe", "d") +
                        knownObjectFileExtensions +
                        knownSourceFileExtensions
    }
}

