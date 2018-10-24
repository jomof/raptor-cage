package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.*
import com.github.jomof.buildserver.server.model.ClangCall
import com.github.jomof.buildserver.server.model.ClangOperation
import com.github.jomof.buildserver.common.io.teleportStdio
import com.github.jomof.buildserver.common.localCacheStoreRoot
import com.github.jomof.buildserver.common.os
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.*

class ClangExecutionKtTest {
    private fun withStdio(call : (ObjectOutputStream) -> Unit) {
        val byteStream = ByteArrayOutputStream()
        val write = ObjectOutputStream(byteStream)
        call(write)
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

    }

    @Test
    fun simpleClangCppToO() {
        localCacheStoreRoot("simpleClangCppToO").deleteRecursively()
        val folder = isolatedTestFolder()
        File(folder, "out").mkdirs()
        val clangArgs = postProcessCppExampleFlags.readLines()
        val clangFlags = ClangCall(clangArgs)
                .withClangExecutable(clangCompilerToolExample.path)
                .withSourceInput(postProcessCppExample.path)
                .withOutput("out/native-lib.cpp.o")

        withStdio { write ->
            clang("simpleClangCppToO", folder.path, clangFlags.rawFlags, write)
        }

        assertThat(File(folder, "out/native-lib.cpp.o").isFile)
                .named(folder.toString())
                .isTrue()
    }

    @Test
    fun clangExecutePlan() {
        localCacheStoreRoot("clangExecutePlan").deleteRecursively()
        val folder = isolatedTestFolder()
        File(folder, "out").mkdirs()
        val clangArgs = postProcessCppExampleFlags.readLines()
        val clangFlags = ClangCall(clangArgs)
                .withClangExecutable(clangCompilerToolExample.path)
                .withSourceInput(postProcessCppExample.path)
                .withOutput("out/native-lib.cpp.o")
        val plan = createPlan()
                .addClangCall(folder, clangFlags)
                .copyOutputsTo("clangExecutePlan")

        withStdio { write ->
            clang(plan, write)
        }

        assertThat(File(folder, "out/native-lib.cpp.o").isFile)
                .named(folder.toString())
                .isTrue()
    }

    @Test
    fun sourceFilesInPostProcessEquivalent() {
        val folder = isolatedTestFolder()
        val flags = ClangCall(clangFlagsExample.readLines())
                .toPostprocessEquivalent(folder)
        val s = os.fileSeparator
        assertThat(flags.sourceFiles)
                .isEqualTo(listOf(folder.path + "${s}CMakeFiles${s}native-lib.dir${s}native-lib.cpp.o.ii"))
    }

    @Test
    fun toPreprocessor() {
        val folder = isolatedTestFolder()
        val flags = ClangCall(postProcessCppExampleFlags.readLines())
                .withOutput("out/native-lib.cpp.o")
                .toPreprocessEquivalent(folder)
        assertThat(flags.isPreprocessorRun).isTrue()
        assertThat(flags.operation).isEqualTo(ClangOperation.CC_TO_II)
        assertThat(flags.lastOutput).isEqualTo(File(folder, "out/native-lib.cpp.o.ii").path)
    }

    @Test
    fun postProcessRemovesIsystem() {
        val folder = isolatedTestFolder()
        val flags = ClangCall(listOf(
                "-isystem=bob", "tom.cpp", "-o=tom.o"))
                .toPostprocessEquivalent(folder)
        val s = os.fileSeparator
        assertThat(flags.flags.map { it.flag })
                .isEqualTo(listOf(folder.path + "${s}tom.o.ii", "-o=tom.o"))
    }

    @Test
    fun preProcessRemovesAssemblerFlags() {
        val folder = isolatedTestFolder()
        val flags = ClangCall(listOf(
                "-Wa,--noexecstack", "tom.cpp", "-o=tom.o"))
                .toPreprocessEquivalent(folder)
        val s = os.fileSeparator
        assertThat(flags.flags.map { it.flag })
                .isEqualTo(listOf(
                        "tom.cpp", "-o=" + folder.path + "${s}tom.o.ii", "-E"))
    }
}