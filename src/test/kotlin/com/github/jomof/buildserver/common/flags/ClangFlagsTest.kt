package com.github.jomof.buildserver.common.flags

import com.github.jomof.buildserver.clangFlagsExample
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ClangFlagsTest {
    private val basicCcFlags = clangFlagsExample.readLines()

    @Test
    fun tryGetCompileFlags() {
        val flags = ClangFlags(basicCcFlags)
        println(flags)
    }

    @Test
    fun fileExtensions() {
        val flags = ClangFlags(basicCcFlags)
        assertThat(flags.fileExtensions)
                .isEqualTo(setOf("exe", "o", "d", "cpp"))
    }

    @Test
    fun sourceFiles() {
        val flags = ClangFlags(basicCcFlags)
        assertThat(flags.sourceFiles)
                .isEqualTo(listOf("C:/Users/jomof/AndroidStudioProjects/AndroidCCacheExample/app/src/main/cpp/native-lib.cpp"))
        assertThat(flags.lastSourceFile)
                .isEqualTo("C:/Users/jomof/AndroidStudioProjects/AndroidCCacheExample/app/src/main/cpp/native-lib.cpp")
    }


    @Test
    fun isCcCompile() {
        val flags = ClangFlags(basicCcFlags)
        assertThat(flags.isCcCompile).isTrue()
        assertThat(flags.isCCompile).isFalse()
    }

    @Test
    fun operation() {
        val flags = ClangFlags(basicCcFlags)
        assertThat(flags.operation).isEqualTo(ClangOperation.CC_TO_O)
    }

    @Test
    fun danglingFlag() {
        val flags = ClangFlags(listOf("-o"))
        assertThat(flags.flags).isEqualTo(listOf(UnidentifiedClangFlag("-o")))
    }

    @Test
    fun oneArgSeparate() {
        val flags = ClangFlags(listOf("-o", "output.o"))
        assertThat(flags.flags).isEqualTo(listOf(OneArgFlag("-o", "output.o", listOf("-o", "output.o"))))
        assertThat(flags.lastOutput).isEqualTo("output.o")
    }

    @Test
    fun oneArgSeparateDouble() {
        val flags = ClangFlags(listOf("--o", "output.o"))
        assertThat(flags.flags).isEqualTo(listOf(OneArgFlag("--o", "output.o", listOf("--o", "output.o"))))
    }

    @Test
    fun oneArgCombinedDouble() {
        val flags = ClangFlags(listOf("--o=output.o"))
        assertThat(flags.flags).isEqualTo(listOf(OneArgFlag("--o", "output.o", listOf("--o=output.o"))))
    }
}