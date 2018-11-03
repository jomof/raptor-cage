package com.github.jomof.buildserver

import com.github.jomof.buildserver.common.os
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.util.*

val resources = File("./src/test/resources").absoluteFile!!
val postProcessCppExample = File(resources, "native-lib.cpp.o.cpp")
val postProcessCppExampleFlags = File(resources, "native-lib.cpp.o.flags")
val clangFlagsExample = File(resources, "clang-flags.txt")
val tools = File("./tools").absoluteFile!!
val clangCompilerToolExample =
        File(getNdkDownloadIfNecessary("r17c"), "toolchains/llvm/prebuilt/${os.tag}-x86_64/bin/clang++${os.exe}")
val sdkFolder = File(tools, "${os.tag}/sdk")
val submodule = File("./submodule").absoluteFile!!
val cmakeRuns = File("./cmake-runs").absoluteFile!!
val cacheInProject = File("./.cache-in-project").absoluteFile!!
val cmakeScripts = File("./cmake").absoluteFile!!
val raptorCageToolchain = File(cmakeScripts, "raptor-cage.android.toolchain.cmake")
val benchmarkSubmodule = File(submodule, "native-scaling-benchmark-template")


fun isolatedTestFolder() : File {
    val folder = File("./build/test-isolated/${Random().nextLong()
            .toString(36).replace("-", "")}")
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

    @Test
    fun checkBenchmarkTemplate() {
        assertThat(benchmarkSubmodule.isDirectory).isTrue()
    }
}