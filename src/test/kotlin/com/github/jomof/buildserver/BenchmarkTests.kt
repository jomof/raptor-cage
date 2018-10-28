package com.github.jomof.buildserver

import com.github.jomof.buildserver.common.io.RemoteStdio
import com.github.jomof.buildserver.common.io.teleportStdio
import com.github.jomof.buildserver.common.os
import com.github.jomof.buildserver.common.process.redirectAndWaitFor
import org.junit.Test
import java.io.*

class BenchmarkTests {

    private fun withStdio(call : (RemoteStdio) -> Unit) {
        val byteStream = ByteArrayOutputStream()
        val write = ObjectOutputStream(byteStream)
        val stdio = RemoteStdio(write)
        call(stdio)
        write.flush()
        val bytes = byteStream.toByteArray()
        val byteInputStream = ByteArrayInputStream(bytes)
        val read = ObjectInputStream(byteInputStream)
        teleportStdio(read) { err, message ->
            if (err) {
                println("ERR: $message")
            } else {
                println("OUT: $message")
            }
        }
    }

    @Test
    fun basic() {
        val sdkCandidates = listOf(
                "C:/android-sdk-windows",
                "C:/Users/jomof/AppData/Local/Android/Sdk")
        val localSdkFolder = sdkCandidates
                .mapNotNull { path ->
                    val folder = File(path)
                    if (folder.isDirectory) {
                        folder
                    } else {
                        null
                    }
                }.first()
        val folder = isolatedTestFolder()
        val localProperties = File(folder, "local.properties")
        val javaExeFolder =
                File(System.getProperties().getProperty("java.home"))

        folder.mkdirs()

        benchmarkSubmodule.copyRecursively(folder)
        println(sdkFolder.toString())
        localProperties.writeText(
                "sdk.dir=${localSdkFolder.path.replace("\\", "/")}")


        fun execute(vararg args : String) {
            withStdio { stdio ->
                try {
                    val process = ProcessBuilder(args.toList())
                            .directory(folder)
                    process.environment()["JAVA_HOME"] = javaExeFolder.path
                    process
                            .start()
                            .redirectAndWaitFor(stdio)
                } finally {
                    stdio.exit()
                }
            }
        }
        execute("./gradlew${os.bat}", "assemble", "clean")
        execute("./gradlew${os.bat}", "assemble")
    }
}