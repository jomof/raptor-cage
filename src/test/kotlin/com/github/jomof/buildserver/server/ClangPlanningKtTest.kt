package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.clangFlagsExample
import com.github.jomof.buildserver.isolatedTestFolder
import com.github.jomof.buildserver.server.model.ClangCall
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class ClangPlanningKtTest {
    private val serverName = "ClangPlanningKtTest"

    private fun simulatePlan(plan : List<PlanStep>) : Map<String, String> {
        val filesMap = mutableMapOf<String, String>()
        val seenStores = mutableMapOf<File, String>()
        plan.onEach { step ->
            when(step) {
                is ExecuteClang -> {
                    step.call.outputFiles().onEach {
                        (type, files) -> files.onEach { file ->
                            require(!filesMap.contains(file))
                            filesMap[file] = type.toString()
                        }
                    }
                }
                is CopyFile -> {
                    require(filesMap.contains(step.from))
                    val storeAlias =
                        if (seenStores.contains(step.toFolder)) {
                            seenStores[step.toFolder]!!
                        } else {
                            seenStores[step.toFolder] = "{store-${seenStores.size}}"
                            seenStores[step.toFolder]!!
                        }
                    val fileAlias = "$storeAlias/${step.toFile}"
                    require(!filesMap.contains(fileAlias))
                    filesMap[fileAlias] = "copy of ${filesMap[step.from]}"
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
        assertThat(result["CMakeFiles/native-lib.dir/native-lib.cpp.o.d"]).isEqualTo("MF")
        assertThat(result["CMakeFiles/native-lib.dir/native-lib.cpp.o"]).isEqualTo("OUTPUT")
        assertThat(result["{store-0}/CMakeFiles/native-lib.dir/native-lib.cpp.o.d"]).isEqualTo("copy of MF")
        assertThat(result["{store-0}/CMakeFiles/native-lib.dir/native-lib.cpp.o"]).isEqualTo("copy of OUTPUT")
    }

    @Test
    fun absoluteOutputFolder() {
        val folder = isolatedTestFolder().absoluteFile
        val absoluteOutput = folder.absolutePath + "out/my-output.o"
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
        assertThat(result["CMakeFiles/native-lib.dir/native-lib.cpp.o.d"]).isEqualTo("MF")
        assertThat(result[absoluteOutput]).isEqualTo("OUTPUT")
        assertThat(result["{store-0}/CMakeFiles/native-lib.dir/native-lib.cpp.o.d"]).isEqualTo("copy of MF")
        assertThat(result["{store-0}/out/my-output.o"]).isEqualTo("copy of OUTPUT")
    }
}