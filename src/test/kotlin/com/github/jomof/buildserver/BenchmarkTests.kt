package com.github.jomof.buildserver

import com.github.jomof.buildserver.common.os
import com.google.common.truth.Truth.assertThat
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
        runMetadataCapture("normal", "", ndk, basis)
        runMetadataCapture("forced", "", ndk,
                basis.withCmakeArguments(
                        "-DCMAKE_CXX_COMPILER_FORCED=true",
                        "-DCMAKE_C_COMPILER_FORCED=true")
        )

        val withClang = basis
                .withCmakeArguments("-DANDROID_TOOLCHAIN=clang")
                .withNdk(ndk)
        runMetadataCapture("normal", "-clang", ndk, withClang)
        runMetadataCapture("forced", "-clang", ndk,
                withClang.withCmakeArguments(
                    "-DCMAKE_CXX_COMPILER_FORCED=true",
                        "-DCMAKE_C_COMPILER_FORCED=true")
        )

        val withGcc = basis
                .withCmakeArguments("-DANDROID_TOOLCHAIN=gcc")
                .withNdk(ndk)
        runMetadataCapture("normal", "-gcc", ndk, withGcc)
        runMetadataCapture("forced", "-gcc", ndk,
                withGcc.withCmakeArguments(
                        "-DCMAKE_CXX_COMPILER_FORCED=true",
                        "-DCMAKE_C_COMPILER_FORCED=true")
        )
    }

    private fun runMetadataCapture(
            type: String,
            subtype: String,
            ndk: String,
            basis: Benchmark) {
        val folder = File(cmakeRuns, type)
        val annotation = when(os.tag) {
            "windows" -> subtype
            else -> "$subtype-${os.tag}"
        }
        val sentinel = File(folder, "$ndk$annotation.txt")
        println(sentinel.path)
        if (!sentinel.exists()) {
            folder.mkdirs()
            val workspace = basis.resetWorkingFolder()
            val run = workspace.prepare()
            val mylibrary = File(run.workingFolder, "mylibrary")
            mylibrary.listFiles { file ->
                file.name.startsWith("variables") && file.name.endsWith(".txt")
            }.toList().onEach { file ->
                val baseName = file.name.substringBefore(".txt") + "$annotation.txt"
                file.copyTo(File(folder, baseName), overwrite = true)
            }
            sentinel.writeText("done")
        }
    }

    data class BucketKey(
            val bucketKeys : List<String>,
            val bucketValues : List<String>,
            val key : String
    )

    @Test
    fun diffs() {
        val normalRuns = File(cmakeRuns, "normal")
        val forcedCompilerRuns = File(cmakeRuns, "forced")

        val normalOnlyKeySet = mutableSetOf<String>()
        val buckets = mutableMapOf<
                BucketKey,
                MutableSet<String>>()

        val bucketKeys : List<List<String>> = listOf(
                listOf(),
                listOf("ANDROID_ABI"),
                listOf("ANDROID_TOOLCHAIN"),
                listOf("ANDROID_TOOLCHAIN", "ANDROID_ABI"),
                listOf("TEST_ANDROID_NDKREVISION"),
                listOf("TEST_ANDROID_NDKREVISION", "ANDROID_ABI"),
                listOf("ANDROID_TOOLCHAIN", "TEST_ANDROID_NDKREVISION"),
                listOf("ANDROID_TOOLCHAIN", "TEST_ANDROID_NDKREVISION", "ANDROID_ABI")
        )

        val substitutions = listOf<String>(
                "ANDROID_SYSROOT",
                "ANDROID_TOOLCHAIN_ROOT",
                "ANDROID_NDK",
                "ANDROID_HEADER_TRIPLE",
                "ANDROID_LLVM_TRIPLE",
                "ANDROID_PLATFORM",
                "CMAKE_CXX_ANDROID_TOOLCHAIN_MACHINE",
                "ANDROID_HOST_TAG",
                "ANDROID_ABI"
//                "ANDROID_TOOLCHAIN"
//                "ANDROID_ARCH_NAME",
//                "ANDROID_TOOLCHAIN_NAME"
        )

        for (normalFile in normalRuns.listFiles()) {
            val forcedFile = File(forcedCompilerRuns, normalFile.name)
            assertThat(forcedFile.isFile).isTrue()
            if (!normalFile.name.startsWith("variables")) continue

            val normalKeys = readKeys(normalFile)
            val forcedKeys = readKeys(forcedFile)
            val normalOnlyKeys = normalKeys.keys subtract forcedKeys.keys
            val bucketValues = bucketKeys.map {
                it.map {key ->
                    normalKeys[key] ?: throw RuntimeException("${normalFile.name} $key")
                }
            }

            for (normalOnlyKey in normalOnlyKeys) {
                normalOnlyKeySet.add(normalOnlyKey)
                for ((outerBucketKeys, bucketValue) in bucketKeys zip bucketValues) {
                    val bucketKey = BucketKey(outerBucketKeys, bucketValue, normalOnlyKey)
                    val prior = buckets[bucketKey] ?: {
                        buckets[bucketKey] = mutableSetOf()
                        buckets[bucketKey]
                    }()!!

                    var value = normalKeys[normalOnlyKey]!!
                    for (substitution in substitutions) {
                        if (!normalKeys.containsKey(substitution)) continue
                        value = value.replace(
                                normalKeys[substitution]!!,
                                "\${$substitution}")
                    }
                    prior.add(value)
                }
            }
        }

        for (normalOnlyKey in normalOnlyKeySet) {
            var satisfied = false
            var allValues = mutableSetOf<String>()
            for (bucketKey in bucketKeys) {
                var max = 0
                val values = mutableSetOf<String>()
                for (bucket in buckets) {
                    if (bucket.key.bucketKeys != bucketKey) continue
                    if (bucket.key.key != normalOnlyKey) continue
                    if (bucket.value.size > max) {
                        max = bucket.value.size
                        values.clear()
                    }
                    values.addAll(bucket.value)
                    allValues.addAll(bucket.value)
                }
                if (max == 1) {
                    println("$normalOnlyKey is satisfied by $bucketKey: $values")
                    satisfied = true
                    break
                }
            }
            if (!satisfied) {
                println(allValues.joinToString("\r\n"))
                throw RuntimeException("$normalOnlyKey was not satisfied")
            }
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

    private fun mapArgumentFiles(beforeLibrary: File): Map<String, Map<String, String>> {
        return beforeLibrary.listFiles { file ->
            file.name.startsWith("variables") && file.name.endsWith(".txt")
        }.map { file ->

            val map = readKeys(file)
            Pair(file.name, map)
        }.toMap()
    }

    private fun readKeys(file: File): MutableMap<String, String> {
        val uniqueKey = file.parentFile.parentFile.name
        val map = mutableMapOf<String, String>()
        var currentKey = ""
        var currentValue = ""
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
        return map
    }
}