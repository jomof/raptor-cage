package com.github.jomof.buildserver.common.flags

import com.github.jomof.buildserver.common.flags.ClangOperation.*

data class ClangFlags(val rawFlags : List<String>) {
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
    val cFileExtensions = (fileExtensions intersect knownCFileExtensions)
    val ccFileExtensions = (fileExtensions intersect knownCcFileExtensions)
    val iFileExtensions = (fileExtensions intersect knownIFileExtensions)
    val iiFileExtensions = (fileExtensions intersect knownIiFileExtensions)
    val objectFileExtensions = (fileExtensions intersect knownObjectFileExtensions)
    val isObjectOutput = !objectFileExtensions.isEmpty()
    val isCcCompile = !ccFileExtensions.isEmpty() && !objectFileExtensions.isEmpty()
    val isCCompile = !cFileExtensions.isEmpty() && !objectFileExtensions.isEmpty()
    val isIiCompile = !iiFileExtensions.isEmpty() && !objectFileExtensions.isEmpty()
    val isICompile = !iFileExtensions.isEmpty() && !objectFileExtensions.isEmpty()
    val isCompile = isCCompile || isCcCompile || isIiCompile || isICompile
    val outputFlags = flags
            .filterIsInstance<OneArgFlag>()
            .filter { it.key == "-o" || it.key == "-o" }
    val outputs = outputFlags.map { it.value }
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

    fun toPreprocessEquivalent() : ClangFlags {
        require(isCompile)
        val preprocessExtension = if (isCcCompile) ".ii" else ".i"
        val flags = flags.map { flag ->
            when {
                flag is OneArgFlag && flag.isFlag("o") ->
                    listOf(flag.key, flag.value + preprocessExtension)
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

    fun toPostprocessEquivalent() : ClangFlags {
        require(isCompile)
        val preprocessExtension = if (isCcCompile) ".ii" else ".i"
        val preprocessFile = lastOutput + preprocessExtension
        val newFlags = flags
                .filter { flag -> !unusedInPostProcessPhase.any { flag.isFlag(it) } }
                .map {
            when(it) {
                is SourceFileFlag -> listOf(preprocessFile)
                else -> it.sourceFlags
            }
        }.flatten()
        return ClangFlags(newFlags)
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
                !flag.startsWith("-")
                        && knownSourceFileExtensions.any { flag.endsWith(it) } ->
                    result.add(SourceFileFlag(flag))
                else -> result.add(UnidentifiedClangFlag(flag))
            }
            ++i
        }
        return result
    }

    fun withClangExecutable(executable : String) : ClangFlags {
        return ClangFlags(listOf(executable) + rawFlags.drop(1))
    }

    fun withSourceInput(source : String) : ClangFlags {
        require(isCompile)
        val newFlags = flags
                .map {
                    when(it) {
                        is SourceFileFlag ->
                            listOf(source)
                        else -> it.sourceFlags
                    }
                }.flatten()
        val result = ClangFlags(newFlags)
        require(result.isCompile)
        return result
    }

    fun withOutput(output : String) : ClangFlags {
        require(isCompile)
        val flags = flags.map { flag ->
            when {
                flag is OneArgFlag && flag.isFlag("o") ->
                    listOf(flag.key, output)
                else -> flag.sourceFlags
            }
        }.flatten()
        return ClangFlags(flags)
    }

    companion object {
        private val unusedInPostProcessPhase = setOf("MD", "MT", "isystem", "MF")
        private val oneArgFlagsNoDash = setOf("o", "MT", "MF", "isystem")
        private val oneArgFlagsSingleDash = oneArgFlagsNoDash.map { "-$it" }
        private val oneArgFlagsDoubleDash = oneArgFlagsNoDash.map { "--$it" }
        private val oneArgFlags = oneArgFlagsSingleDash + oneArgFlagsDoubleDash
        private val oneArgFlagsEquals = oneArgFlags.map { "$it=" }
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

abstract class ClangFlag {
    abstract val sourceFlags : List<String>
    abstract val flag : String
    abstract fun isFlag(flag : String) : Boolean
}

data class OneArgFlag(
        val key : String,
        val value : String,
        override val sourceFlags : List<String>) : ClangFlag() {
    override val flag = "$key $value"
    override fun isFlag(flag : String)  =
            when(key) {
                "--$flag", "-$flag" ->
                    true
                else ->
                    false
            }
}

data class SourceFileFlag(
        val sourceFile : String) : ClangFlag() {
    override val sourceFlags = listOf(sourceFile)
    override val flag = sourceFile
    override fun isFlag(flag : String) = false
}

data class UnidentifiedClangFlag(val rawFlag : String) : ClangFlag() {
    override val sourceFlags = listOf(rawFlag)
    override val flag = rawFlag
    override fun isFlag(flag : String)  =
            when(rawFlag) {
                "--$flag", "-$flag" ->
                    true
                else ->
                    false
            }
}

