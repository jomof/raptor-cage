package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.*
import com.github.jomof.buildserver.common.flags.ClangFlags
import com.github.jomof.buildserver.common.flags.ClangOperation
import com.github.jomof.buildserver.common.io.teleportStdio
import com.github.jomof.buildserver.common.localCacheStoreRoot
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.*

class ClangExecutionKtTest {

    @Test
    fun simpleClangIiToO() {
        val folder = isolatedTestFolder()
        val clangArgs = postProcessCppExampleFlags.readLines()
        val clangFlags = ClangFlags(clangArgs)
                .withClangExecutable(clangCompilerToolExample.path)
                .toPostprocessEquivalent()
                .withSourceInput(postProcessIiExample.path)
                .withOutput("native-lib.cpp.o")

        val byteStream = ByteArrayOutputStream()
        val write = ObjectOutputStream(byteStream)
        clang("simpleClangIiToO", folder.path, clangFlags.rawFlags, write)
        write.flush()
        val bytes = byteStream.toByteArray()
        val byteInputStream = ByteArrayInputStream(bytes)
        val read = ObjectInputStream(byteInputStream)
        val sb = StringBuilder()
        teleportStdio(read) {
            err, message ->
            if (err) {
                sb.append("ERR: $message \n") }
            else {
                sb.append("OUT: $message \n") }
        }

        println(sb)
        assertThat(File(folder, "native-lib.cpp.o").isFile).isTrue()
    }

    @Test
    fun simpleClangCppToO() {
        localCacheStoreRoot("simpleClangCppToO").deleteRecursively()
        val folder = isolatedTestFolder()
        val clangArgs = postProcessCppExampleFlags.readLines()
        val clangFlags = ClangFlags(clangArgs)
                .withClangExecutable(clangCompilerToolExample.path)
                .withSourceInput(postProcessCppExample.path)
                .withOutput("native-lib.cpp.o")

        val byteStream = ByteArrayOutputStream()
        val write = ObjectOutputStream(byteStream)
        clang("simpleClangCppToO", folder.path, clangFlags.rawFlags, write)
        write.flush()
        val bytes = byteStream.toByteArray()
        val byteInputStream = ByteArrayInputStream(bytes)
        val read = ObjectInputStream(byteInputStream)
        val sb = StringBuilder()
        teleportStdio(read) {
            err, message ->
            if (err) {
                sb.append("ERR: $message \n") }
            else {
                sb.append("OUT: $message \n") }
        }
        println(sb)
        assertThat(File(folder, "native-lib.cpp.o").isFile).isTrue()
    }

    @Test
    fun sourceFilesInPostProcessEquivalent() {
        val flags = ClangFlags(clangFlagsExample.readLines()).toPostprocessEquivalent()
        assertThat(flags.sourceFiles)
                .isEqualTo(listOf("CMakeFiles/native-lib.dir/native-lib.cpp.o.ii"))
        assertThat(flags.lastSourceFile)
                .isEqualTo("CMakeFiles/native-lib.dir/native-lib.cpp.o.ii")
    }

    @Test
    fun toPreprocessor() {
        val flags = ClangFlags(clangFlagsExample.readLines()).toPreprocessEquivalent()
        assertThat(flags.isPreprocessorRun).isTrue()
        assertThat(flags.operation).isEqualTo(ClangOperation.CC_TO_II)
        assertThat(flags.lastOutput).endsWith(".ii")
    }

    @Test
    fun postProcessRemovesIsystem() {
        val flags = ClangFlags(listOf(
                "-isystem=bob", "tom.cpp", "-o=tom.o"))
                .toPostprocessEquivalent()
        assertThat(flags.flags.map { it.flag })
                .isEqualTo(listOf("tom.o.ii", "-o tom.o"))
    }
}