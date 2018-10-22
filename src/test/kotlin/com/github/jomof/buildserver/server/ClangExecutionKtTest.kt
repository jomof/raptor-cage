package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.*
import com.github.jomof.buildserver.common.flags.ClangFlags
import com.github.jomof.buildserver.common.flags.ClangOperation
import com.github.jomof.buildserver.common.io.teleportStdio
import com.github.jomof.buildserver.common.localCacheStoreRoot
import com.github.jomof.buildserver.common.os
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.*

class ClangExecutionKtTest {
    @Test
    fun simpleClangCppToO() {
        localCacheStoreRoot("simpleClangCppToO").deleteRecursively()
        val folder = isolatedTestFolder()
        val clangArgs = postProcessCppExampleFlags.readLines()
        val clangFlags = ClangFlags(clangArgs)
                .withClangExecutable(clangCompilerToolExample.path)
                .withSourceInput(postProcessCppExample.path)
                .withOutput("out/native-lib.cpp.o")

        val byteStream = ByteArrayOutputStream()
        val write = ObjectOutputStream(byteStream)
        clang("simpleClangCppToO", folder.path, clangFlags.rawFlags, write)
        write.flush()
        val bytes = byteStream.toByteArray()
        val byteInputStream = ByteArrayInputStream(bytes)
        val read = ObjectInputStream(byteInputStream)
        val sb = StringBuilder()
        teleportStdio(read) { err, message ->
            if (err) {
                sb.append("ERR: $message \n")
            } else {
                sb.append("OUT: $message \n")
            }
        }
        println(sb)
        assertThat(File(folder, "out/native-lib.cpp.o").isFile)
                .named(folder.toString())
                .isTrue()
    }

    @Test
    fun sourceFilesInPostProcessEquivalent() {
        val folder = isolatedTestFolder()
        val flags = ClangFlags(clangFlagsExample.readLines())
                .toPostprocessEquivalent(folder)
        val s = os.fileSeparator
        assertThat(flags.sourceFiles)
                .isEqualTo(listOf(folder.path + "${s}CMakeFiles${s}native-lib.dir${s}native-lib.cpp.o.ii"))
    }

    @Test
    fun toPreprocessor() {
        val folder = isolatedTestFolder()
        val flags = ClangFlags(postProcessCppExampleFlags.readLines())
                .withOutput("out/native-lib.cpp.o")
                .toPreprocessEquivalent(folder)
        assertThat(flags.isPreprocessorRun).isTrue()
        assertThat(flags.operation).isEqualTo(ClangOperation.CC_TO_II)
        assertThat(flags.lastOutput).isEqualTo(File(folder, "out/native-lib.cpp.o.ii").path)
    }

    @Test
    fun postProcessRemovesIsystem() {
        val folder = isolatedTestFolder()
        val flags = ClangFlags(listOf(
                "-isystem=bob", "tom.cpp", "-o=tom.o"))
                .toPostprocessEquivalent(folder)
        val s = os.fileSeparator
        assertThat(flags.flags.map { it.flag })
                .isEqualTo(listOf(folder.path + "${s}tom.o.ii", "-o tom.o"))
    }
}