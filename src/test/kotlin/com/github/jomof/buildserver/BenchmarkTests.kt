package com.github.jomof.buildserver

import com.github.jomof.buildserver.common.os
import org.junit.Test

class BenchmarkTests {
    @Test
    fun basic() {
        Benchmark(moduleCount = 1)
            .prepare()
            .execute("./gradlew${os.bat}", "assemble", "clean")
            .execute("./gradlew${os.bat}", "assemble")
    }

    @Test
    fun ndks() {
        val ndks = listOf("r17b")
        //val ndks = listOf("r13b", "r14b", "r15c", "r16b", "r17c")
        ndks.onEach { ndk ->
            val path = getNdkDownloadIfNecessary(ndk)
            System.err.println("Downloaded and unzipped $path")
        }
    }
}