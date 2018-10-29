package com.github.jomof.buildserver

import com.github.jomof.buildserver.common.os
import org.junit.Test

class BenchmarkTests {
    @Test
    fun basic() {
        Benchmark(moduleCount = 2)
            .prepare()
            .execute("./gradlew${os.bat}", "assemble", "clean")
            .execute("./gradlew${os.bat}", "assemble")
    }
}