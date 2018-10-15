package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.clangCompilerToolExample
import com.github.jomof.buildserver.clangFlagsExample
import com.github.jomof.buildserver.common.flags.ClangFlags
import com.github.jomof.buildserver.common.io.teleportStdio
import com.github.jomof.buildserver.postProcessCppExample
import org.junit.Test
import java.io.*

class ClangExecutionKtTest {

    @Test
    fun simpleClang() {
        val clangArgs = clangFlagsExample.readLines()
        val clangFlags = ClangFlags(clangArgs)
                .withClangExecutable(clangCompilerToolExample.path)
                .toPostprocessEquivalent()
                .withSourceInput(postProcessCppExample.path)
                .withOutput("native-lib.cpp.o")

        val byteStream = ByteArrayOutputStream()
        val write = ObjectOutputStream(byteStream)
        clang(File(".").path, clangFlags.rawFlags, write)
        write.flush()
        val bytes = byteStream.toByteArray()
        val byteInputStream = ByteArrayInputStream(bytes)
        val read = ObjectInputStream(byteInputStream)
        val sb = StringBuilder()
        teleportStdio(read) {
            err, message ->
            if (err) {
                sb.append("ERR: $message \n") }
            else { sb.append("OUT: $message \n") }
        }

        println(sb)

    }
}