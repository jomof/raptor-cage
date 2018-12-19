package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.clangFlagsExample
import com.github.jomof.buildserver.common.ServerName
import com.github.jomof.buildserver.isolatedTestFolder
import com.github.jomof.buildserver.server.model.ClangCall
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class ClangPlanningKtTest {
    private val serverName = ServerName("ClangPlanningKtTest")

    private fun simulatePlan(plan : List<PlanStep>) : Map<String, String> {
        val filesMap = mutableMapOf<String, String>()
        val seenWorkingDirs = mutableMapOf<File, String>()
        val seenStores = mutableMapOf<File, String>()

        fun workingDirAlias(workingDir : File, file : String) : String {
            if (!seenWorkingDirs.contains(workingDir)) {
                seenWorkingDirs[workingDir] = "{working-${seenStores.size}}"
            }
            return seenWorkingDirs[workingDir]!! + "/$file"
        }

        fun storeDirAlias(storeDir : File, file : String) : String {
            if (!seenStores.contains(storeDir)) {
                seenStores[storeDir] = "{store-${seenStores.size}}"
            }
            return seenStores[storeDir]!! + "/$file"
        }

        plan.onEach { step ->
            when(step) {
                is ExecuteClang -> {
                    step.call.outputFiles().onEach {
                        (type, files) -> files.onEach { file ->
                            val relative =
                            if (file.startsWith(step.workingFolder.path)) {
                                file.substringAfter(step.workingFolder.path).substring(1)
                            } else {
                                file
                            }
                            val workingFile = workingDirAlias(step.workingFolder, relative)
                            require(!filesMap.contains(workingFile))
                            filesMap[workingFile] = type.toString()
                        }
                    }
                }
                is CopyFile -> {
                    val fromFile = workingDirAlias(step.fromFolder, step.fromFile)
                    val toFile = storeDirAlias(step.toFolder, step.toFile)
                    require(filesMap.contains(fromFile)) {
                        "$fromFile was not already seen"}
                    require(!filesMap.contains(toFile)) { "$toFile was already seen"}
                    filesMap[toFile] = "copy of ${filesMap[fromFile]}"
                }
            }
        }
        return filesMap
    }

    @Test
    fun simple() {
        val plan = createPlan()
                .addClangCall(
                        File("my-working-dir"),
                        ClangCall(clangFlagsExample.readLines()))
                .copyOutputsTo(serverName)
        assertThat(plan[0] is ExecuteClang).isTrue()
        val result = simulatePlan(plan)
        println(result.toString())
        assertThat(result).hasSize(4)
        assertThat(result["{working-0}/CMakeFiles/native-lib.dir/native-lib.cpp.o.d"]).isEqualTo("MF")
        assertThat(result["{working-0}/CMakeFiles/native-lib.dir/native-lib.cpp.o"]).isEqualTo("OUTPUT")
        assertThat(result["{store-0}/CMakeFiles/native-lib.dir/native-lib.cpp.o.d"]).isEqualTo("copy of MF")
        assertThat(result["{store-0}/CMakeFiles/native-lib.dir/native-lib.cpp.o"]).isEqualTo("copy of OUTPUT")
    }

    @Test
    fun absoluteOutputFolder() {
        val folder = isolatedTestFolder().absoluteFile
        val absoluteOutput = folder.absolutePath + "/out/my-output.o"
        val call = ClangCall(clangFlagsExample.readLines())
                .withOutput(absoluteOutput)
        val plan = createPlan()
                .addClangCall(
                        folder,
                        call)
                .copyOutputsTo(serverName)
        assertThat(plan[0] is ExecuteClang).isTrue()
        val result = simulatePlan(plan)
        println(result.toString())
        assertThat(result).hasSize(4)
        assertThat(result["{working-0}/CMakeFiles/native-lib.dir/native-lib.cpp.o.d"]).isEqualTo("MF")
        assertThat(result["{working-0}/out/my-output.o"]).isEqualTo("OUTPUT")
        assertThat(result["{store-0}/CMakeFiles/native-lib.dir/native-lib.cpp.o.d"]).isEqualTo("copy of MF")
        assertThat(result["{store-0}/out/my-output.o"]).isEqualTo("copy of OUTPUT")
    }
}