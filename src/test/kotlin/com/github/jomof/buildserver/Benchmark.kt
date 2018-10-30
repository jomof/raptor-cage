package com.github.jomof.buildserver

import com.github.jomof.buildserver.common.io.RemoteStdio
import com.github.jomof.buildserver.common.io.teleportStdio
import com.github.jomof.buildserver.common.os
import com.github.jomof.buildserver.common.process.redirectAndWaitFor
import java.io.*

data class Benchmark(
        val sdkFolder : String = sdkFolder(),
        val ndkFolder : String = getNdkDownloadIfNecessary("r17c").path,
        val javaHome : String = System.getProperties().getProperty("java.home"),
        val benchmarkSource : File = benchmarkSubmodule,
        val workingFolder : File = isolatedTestFolder(),
        val moduleCount : Int = 2,
        val cmakeArguments : String = "") {

    private fun withStdio(call : (RemoteStdio) -> Unit) {
        val byteStream = ByteArrayOutputStream()
        val write = ObjectOutputStream(byteStream)
        val stdio = RemoteStdio(write)
        call(stdio)
        write.flush()
        val bytes = byteStream.toByteArray()
        val byteInputStream = ByteArrayInputStream(bytes)
        val read = ObjectInputStream(byteInputStream)
        var lastTimeStamp = System.currentTimeMillis()
        teleportStdio(read) { err, message ->
            val currentTimestamp = System.currentTimeMillis()
            val elapsed = synchronized(lastTimeStamp) {
                val diff = currentTimestamp - lastTimeStamp
                lastTimeStamp = currentTimestamp
                diff
            }
            val time = if (elapsed > 10) {
                "[[$elapsed]]"
            } else {
                ""
            }
            if (err) {
                println("ERR$time: $message")
            } else {
                println("OUT$time: $message")
            }
        }
    }

    fun withNdk(ndk : String) : Benchmark {
        return copy(ndkFolder = getNdkDownloadIfNecessary(ndk).path)
    }

    fun resetWorkingFolder() : Benchmark {
        return this.copy(workingFolder = isolatedTestFolder())
    }

    fun prepare() : Benchmark {
        val localProperties = File(workingFolder, "local.properties")
        val settingsGradle = File(workingFolder, "settings.gradle")
        benchmarkSubmodule.copyRecursively(workingFolder)
        println(com.github.jomof.buildserver.sdkFolder.toString())
        val ndkFolder = ndkFolder.replace("\\", "/")
        localProperties.writeText(
                "sdk.dir=$sdkFolder\n" +
                "ndk.dir=$ndkFolder\n")
        val sourceLibrary = File(workingFolder, "mylibrary")

        if (!cmakeArguments.isEmpty()){
            val sourceLibraryBuildGradle = File(sourceLibrary, "build.gradle")
            sourceLibraryBuildGradle
                    .writeText(sourceLibraryBuildGradle.readText()
                        .replace("//<<arguments>>", "arguments $cmakeArguments"))
        }

        if (!sourceLibrary.isDirectory) {
            System.err.println("Directory $sourceLibrary did not exist.")
            assert(sourceLibrary.isDirectory)
        }

        if (os.tag != "windows") {
            for (file in workingFolder.walk()) {
                when(file.name) {
                    "gradlew", "clang++" -> {
                        file.setExecutable(true)
                    }
                    else -> {}
                }
            }
        }

        execute("./gradlew${os.bat}", "assemble")

        val settingsGradleText = StringBuilder("include ':app', ':mylibrary'")
        (2 ..  moduleCount).onEach { i ->
            val libraryName = "mylibrary-$i"
            val moduleFolder = File(workingFolder, libraryName)
            sourceLibrary.copyRecursively(moduleFolder)
            settingsGradleText.append(", ':$libraryName'")
        }
        settingsGradle.writeText(settingsGradleText.toString())
        return this
    }

    fun withCmakeArguments(arguments : String) : Benchmark{
        return this.copy(cmakeArguments = arguments)
    }

    fun execute(vararg args : String) : Benchmark {
        withStdio { stdio ->
            try {
                val process = ProcessBuilder(args.toList())
                        .directory(workingFolder)
                process.environment()["JAVA_HOME"] = javaHome
                process
                        .start()
                        .redirectAndWaitFor(stdio)
            } finally {
                stdio.exit()
            }
        }
        return this
    }
}

private fun sdkFolder() : String {
    val sdkCandidates = listOf(
            "/usr/local/google/home/jomof/Android/Sdk",
            "C:/android-sdk-windows",
            "C:/Users/jomof/AppData/Local/Android/Sdk")
    return sdkCandidates
            .mapNotNull { path ->
                val folder = File(path)
                if (folder.isDirectory) {
                    folder
                } else {
                    null
                }
            }.first().path.replace("\\", "/")
}