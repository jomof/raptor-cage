package com.github.jomof.buildserver.common.flags

import com.github.jomof.buildserver.common.flags.ClangOperation.*

data class ClangFlags(val rawFlags : List<String>) {
    val flags = interpretFlags(rawFlags)
    val fileExtensions = flags
            .map { flag -> flag.flag.substringAfterLast(".", "") }
            .filter { extension -> knownFileExtensions.contains(extension) }
            .toSet()
    val isPreprocessorRun = rawFlags.contains("-E")
    val cFileExtensions = (fileExtensions intersect knownCFileExtensions)
    val ccFileExtensions = (fileExtensions intersect knownCcFileExtensions)
    val objectFileExtensions = (fileExtensions intersect knownObjectFileExtensions)
    val isObjectOutput = !objectFileExtensions.isEmpty()
    val isCcCompile = !ccFileExtensions.isEmpty() && !objectFileExtensions.isEmpty()
    val isCCompile = !cFileExtensions.isEmpty() && !objectFileExtensions.isEmpty()
    val isCompile = isCCompile || isCcCompile
    val outputFlags = flags
            .filterIsInstance<OneArgFlag>()
            .filter { it.key == "-o" || it.key == "-o" }
    val outputs = outputFlags.map { it.value }
    val lastOutput = outputs.lastOrNull()

    val operation = when {
        !isPreprocessorRun && isCCompile && isObjectOutput -> C_TO_O
        !isPreprocessorRun && isCcCompile && isObjectOutput -> CC_TO_O
        isPreprocessorRun && isCCompile && isObjectOutput -> C_TO_I
        isPreprocessorRun && isCcCompile && isObjectOutput -> CC_TO_II
        else -> UNKNOWN
    }

    fun toPreprocessorEquivalent() : ClangFlags {
        when (operation) {
            CC_TO_O, C_TO_O -> {
                val flags = flags.map { flag ->
                    when {
                        flag is OneArgFlag && flag.isFlag("o") ->
                            listOf(flag.key, flag.value + when(operation) {
                                C_TO_O -> ".i"
                                else -> ".ii"
                            })
                        else -> flag.sourceFlags
                    }
                }.flatten()
                return ClangFlags(flags + "-E")
            }
            else -> throw RuntimeException(operation.toString())
        }
    }

    private fun interpretFlags(flags : List<String>) : List<ClangFlag> {
        val result = mutableListOf<ClangFlag>()
        var i = 0
        while (i < flags.size) {
            val flag = flags[i]
            when {
                flags.size != i + 1 && oneArgFlags.contains(flag) -> {
                    result.add(OneArgFlag(
                            flag,
                            flags[i + 1],
                            listOf(flag, flags[i + 1])))
                    ++i
                }
                oneArgFlagsEquals.any { flag.startsWith(it) } -> {
                    val key = flag.substringBefore("=")
                    val value = flag.substringAfter("=")
                    result.add(OneArgFlag(
                            key,
                            value,
                            listOf(flag)))
                }
                else -> result.add(UnidentifiedClangFlag(flag))
            }
            ++i
        }
        return result
    }

    companion object {
        private val oneArgFlagsNoDash = setOf("o", "MT")
        private val oneArgFlagsSingleDash = oneArgFlagsNoDash.map { "-$it" }
        private val oneArgFlagsDoubleDash = oneArgFlagsNoDash.map { "--$it" }
        private val oneArgFlags = oneArgFlagsSingleDash + oneArgFlagsDoubleDash
        private val oneArgFlagsEquals = oneArgFlags.map { "$it=" }
        private val knownCFileExtensions = setOf("c")
        private val knownCcFileExtensions = setOf("c++", "cpp", "cc")
        private val knownObjectFileExtensions = setOf("o")
        val knownFileExtensions =
                setOf("exe", "d") +
                        knownObjectFileExtensions +
                        knownCcFileExtensions +
                        knownCFileExtensions
    }
}

abstract class ClangFlag {
    abstract val sourceFlags : List<String>
    abstract val flag : String
}

data class OneArgFlag(
        val key : String,
        val value : String,
        override val sourceFlags : List<String>) : ClangFlag() {
    override val flag = "$key $value"
    fun isFlag(flag : String) = key == "--$flag" || key == "-$flag"
}

data class UnidentifiedClangFlag(val rawFlag : String) : ClangFlag() {
    override val sourceFlags = listOf(rawFlag)
    override val flag = rawFlag
}

