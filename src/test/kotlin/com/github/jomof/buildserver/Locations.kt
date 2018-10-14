package com.github.jomof.buildserver

import com.github.jomof.buildserver.common.os
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

val resources = File("./src/test/resources").absoluteFile!!
val postProcessCppExample = File(resources, "native-lib.cpp.o.ii")
val tools = File("./tools")
val clangCompilerToolExample = File(tools, "${os.tag}/ndk/18.0/toolchains/llvm/prebuilt/${os.tag}-x86_64/bin/clang++")

class Locations{
    @Test
    fun checkResources() {
        assertThat(resources.isDirectory).isTrue()
        assertThat(postProcessCppExample.isFile).isTrue()
    }

    @Test
    fun checkTools() {
        assertThat(tools.isDirectory).isTrue()
        assertThat(clangCompilerToolExample.isFile).isTrue()
    }
}