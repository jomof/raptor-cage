package com.github.jomof.buildserver.common.io

import java.io.ObjectOutputStream

class RemoteStdio(private val write : ObjectOutputStream) {
    fun stderr(message : String) {
        write.writeByte(PIPE_STDERR)
        write.writeUTF(message)
    }

    fun stdout(message : String) {
        write.writeByte(PIPE_STDOUT)
        write.writeUTF(message)
    }

    fun exit() {
        write.writeByte(PIPE_EXIT)
    }
}