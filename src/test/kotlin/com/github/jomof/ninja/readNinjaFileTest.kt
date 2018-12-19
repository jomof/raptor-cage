package com.github.jomof.ninja

import com.github.jomof.buildserver.Benchmark
import com.github.jomof.buildserver.common.os
import org.junit.Test

import java.io.File

class ReadNinjaFileKtTest {

    @Test
    fun readNinjaFileTest() {
        val benchMark = Benchmark(moduleCount = 1)
                .prepare()
                .execute("./gradlew${os.bat}", "--parallel",
                        "generateJsonModelRelease", "generateJsonModelDebug")
        val projectDir = benchMark.workingFolder
        println(projectDir.toString())
        val ninjas = mutableMapOf<File, NinjaFileDef>()
        projectDir.walkTopDown().forEach { file ->
            if (file.name == "build.ninja")  {
                println(file)
                ninjas[file] = readNinjaFile(file)
            }
        }
        println(ninjas.size)
    }
}