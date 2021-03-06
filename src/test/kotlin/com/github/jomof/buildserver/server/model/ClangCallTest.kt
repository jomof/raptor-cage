package com.github.jomof.buildserver.server.model

import com.github.jomof.buildserver.clangFlagsExample
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ClangCallTest {
    private val basicCcFlags = clangFlagsExample.readLines()

    @Test
    fun tryGetCompileFlags() {
        val flags = ClangCall(basicCcFlags)
        println(flags)
    }

    @Test
    fun fileExtensions() {
        val flags = ClangCall(basicCcFlags)
        assertThat(flags.fileExtensions)
                .isEqualTo(setOf("exe", "o", "d", "cpp"))
    }

    @Test
    fun sourceFiles() {
        val flags = ClangCall(basicCcFlags)
        assertThat(flags.sourceFiles)
                .isEqualTo(listOf("C:/Users/jomof/AndroidStudioProjects/AndroidCCacheExample/app/src/main/cpp/native-lib.cpp"))
        assertThat(flags.lastSourceFile)
                .isEqualTo("C:/Users/jomof/AndroidStudioProjects/AndroidCCacheExample/app/src/main/cpp/native-lib.cpp")
    }


    @Test
    fun isCcCompile() {
        val flags = ClangCall(basicCcFlags)
        assertThat(flags.isCcCompile).isTrue()
        assertThat(flags.isCCompile).isFalse()
    }

    @Test
    fun operation() {
        val flags = ClangCall(basicCcFlags)
        assertThat(flags.operation).isEqualTo(ClangOperation.CC_TO_O)
    }

    @Test
    fun danglingFlag() {
        val flags = ClangCall(listOf("-o"))
        assertThat(flags.flags).isEqualTo(listOf(UnidentifiedClangFlag("-o")))
    }

    @Test
    fun oneArgSeparate() {
        val flags = ClangCall(listOf("-o", "output.o"))
        assertThat(flags.flags).isEqualTo(listOf(
                OneArgFlag(
                        "-o",
                        "output.o",
                        listOf("-o", "output.o"),
                        ClangFlagType.OUTPUT
                        )))
        assertThat(flags.lastOutput).isEqualTo("output.o")
    }

    @Test
    fun identifyOutputFiles() {
        val call = ClangCall(basicCcFlags)
        val outputs = call.outputFiles()
        assertThat(outputs).hasSize(2)
        assertThat(outputs[ClangFlagType.MF]!!.single()).isEqualTo(
                "CMakeFiles/native-lib.dir/native-lib.cpp.o.d")
        assertThat(outputs[ClangFlagType.OUTPUT]!!.single()).isEqualTo(
                "CMakeFiles/native-lib.dir/native-lib.cpp.o")
    }
}