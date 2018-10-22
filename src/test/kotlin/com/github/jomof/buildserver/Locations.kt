package com.github.jomof.buildserver

import com.github.jomof.buildserver.common.os
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.util.*

val resources = File("./src/test/resources").absoluteFile!!
val postProcessCppExample = File(resources, "native-lib.cpp.o.cpp")
val postProcessIiExample = File(resources, "native-lib.cpp.o.ii")
val postProcessCppExampleFlags = File(resources, "native-lib.cpp.o.flags")
val clangFlagsExample = File(resources, "clang-flags.txt")
val tools = File("./tools").absoluteFile
val clangCompilerToolExample = File(tools, "${os.tag}/ndk/18.0/toolchains/llvm/prebuilt/${os.tag}-x86_64/bin/clang++${os.exe}")


fun isolatedTestFolder() : File {
    val folder = File("./build/test-isolated/${Random().nextLong()}")
    folder.mkdirs()
    return folder
}

class Locations{
    @Test
    fun checkResources() {
        assertThat(resources.isDirectory).isTrue()
        assertThat(postProcessCppExample.isFile).isTrue()
    }

    @Test
    fun checkTools() {
        assertThat(tools.isDirectory).isTrue()
        assertThat(clangCompilerToolExample.isFile)
                .named(clangCompilerToolExample.toString())
                .isTrue()
    }
}