package com.github.jomof.buildserver.ninja

import org.junit.Test

import java.io.File

class ReadNinjaFileKtTest {

    @Test
    fun readNinjaFileTest() {

        val projectDir = File("C:\\Users\\Jomo\\IdeaProjects\\raptor-cage\\build\\test-isolated\\nthyuuwa9coe")
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