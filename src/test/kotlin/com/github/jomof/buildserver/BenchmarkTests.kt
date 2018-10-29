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
        Benchmark(moduleCount = 20)
            .prepare()
            .execute("./gradlew${os.bat}", "assemble", "clean")
            .execute("./gradlew${os.bat}", "assemble")
    }
}