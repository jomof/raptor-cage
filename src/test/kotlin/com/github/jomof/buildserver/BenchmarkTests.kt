package com.github.jomof.buildserver

import com.github.jomof.buildserver.common.os
import org.junit.Test
import java.io.File

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
        val ndks = listOf(
                "r13", "r13b",
                "r14", "r14b",
                "r15", "r15b", "r15c",
                "r16", "r16b",
                "r17", "r17b",
                "r17c")
        ndks.onEach { ndk ->
            gatherCmakeMetadata(ndk)
        }
    }

    private fun gatherCmakeMetadata(ndk : String) {
        val basis = Benchmark(moduleCount = 1)
                .withNdk(ndk)
        runBenchmark("normal", ndk, basis)
        runBenchmark("forced", ndk, basis.withCmakeArguments(
                "\"-DCMAKE_CXX_COMPILER_FORCED=true\", " +
                        "\"-DCMAKE_C_COMPILER_FORCED=true\"")
        )
    }

    private fun runBenchmark(type: String, ndk: String, basis: Benchmark) {
        val folder = File(cmakeRuns, type)
        val sentinel = if (os.tag == "windows") { File(folder, "$ndk.txt") }
            else { File(folder, "$ndk-${os.tag}.txt") }
        println(sentinel.path)
        if (!sentinel.exists()) {
            folder.mkdirs()
            val workspace = basis.resetWorkingFolder()
            val run = workspace.prepare()
            val mylibrary = File(run.workingFolder, "mylibrary")
            mylibrary.listFiles { file ->
                file.name.startsWith("variables") && file.name.endsWith(".txt")
            }.toList().onEach { file ->
                val baseName = file.name.substringBefore(".txt") + "-" + os.tag + ".txt"
                file.copyTo(File(folder, baseName))
            }
            sentinel.writeText("done")
        }
    }

    //@Test
    fun slurp() {
        val before = Benchmark(moduleCount = 1)
                .prepare()
        val beforeLibrary = File(before.workingFolder, "mylibrary")
        val beforeMap = mapArgumentFiles(beforeLibrary)
        val after = Benchmark(moduleCount = 1)
                .withCmakeArguments(
                        "\"-DCMAKE_CXX_COMPILER_FORCED=true\", " +
                                "\"-DCMAKE_C_COMPILER_FORCED=true\"")
                .prepare()
        val afterLibrary = File(after.workingFolder, "mylibrary")
        val afterMap = mapArgumentFiles(afterLibrary)

        val nameKeys = beforeMap.keys + afterMap.keys
        for (nameKey in nameKeys) {
            val before = beforeMap[nameKey]!!
            val after = afterMap[nameKey]!!
            reportDifferences(before, after)
        }
    }

    private fun reportDifferences(
            before: Map<String, String>,
            after: Map<String, String>) {
        val onlyBefore = before.keys subtract after.keys
        val onlyAfter = after.keys subtract before.keys
        val common = before.keys intersect after.keys
        onlyAfter.onEach { key -> println("only after $key=${after[key]}") }
        onlyBefore.onEach { key -> println("only before $key=${before[key]}") }
        common.onEach { key ->
            val beforeValue = before[key]!!
            val afterValue = after[key]!!
            if (beforeValue != afterValue) {
                println("changed $key from $beforeValue to $afterValue")
            }
        }
    }

    private fun mapArgumentFiles(beforeLibrary: File): Map<String, MutableMap<String, String>> {
        return beforeLibrary.listFiles { file ->
            file.name.startsWith("variables") && file.name.endsWith(".txt")
        }.map { file ->
            var currentKey = ""
            var currentValue = ""
            val map = mutableMapOf<String, String>()
            val uniqueKey = file.parentFile.parentFile.name
            for (line in file.readLines()) {
                if (line.startsWith("set ")) {
                    if (!currentKey.isEmpty() && !currentValue.isEmpty()) {
                        map[currentKey] = currentValue
                                .replace(uniqueKey, "<unique-folder>")
                    }
                    val noset = line.substringAfter("set ")
                    currentKey = noset.substringBefore("=")
                    currentValue = noset.substringAfter("=")
                } else {
                    currentValue += line
                }
            }
            map[currentKey] = currentValue
            Pair(file.name, map)
        }.toMap()
    }
}