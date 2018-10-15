package com.github.jomof.buildserver.common.io

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class RemoteStdioTest {

    @Test
    fun stdio() {
        val byteStream = ByteArrayOutputStream()
        val write = ObjectOutputStream(byteStream)
        val stdio = RemoteStdio(write)
        stdio.stdout("stdout")
        stdio.stderr("stderr")
        stdio.exit()
        write.flush()
        val bytes = byteStream.toByteArray()
        val byteInputStream = ByteArrayInputStream(bytes)
        val read = ObjectInputStream(byteInputStream)
        val sb = StringBuilder()
        teleportStdio(read) {
            err, message ->
            if (err) { sb.append("ERR: $message ") }
            else { sb.append("OUT: $message ") }
        }
        assertThat(sb.toString()).isEqualTo("OUT: stdout ERR: stderr ")
    }
}