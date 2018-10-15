package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.*
import com.github.jomof.buildserver.common.flags.ClangFlags
import com.github.jomof.buildserver.common.io.teleportStdio
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
        clang(folder.path, clangFlags.rawFlags, write)
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
        val folder = isolatedTestFolder()
        val clangArgs = postProcessCppExampleFlags.readLines()
        val clangFlags = ClangFlags(clangArgs)
                .withClangExecutable(clangCompilerToolExample.path)
                .withSourceInput(postProcessCppExample.path)
                .withOutput("native-lib.cpp.o")

        val byteStream = ByteArrayOutputStream()
        val write = ObjectOutputStream(byteStream)
        clang(folder.path, clangFlags.rawFlags, write)
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
}