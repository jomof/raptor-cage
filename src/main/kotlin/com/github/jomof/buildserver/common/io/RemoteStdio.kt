package com.github.jomof.buildserver.common.io

import java.io.ObjectInputStream
import java.io.ObjectOutputStream

const val PIPE_STDERR = 0
const val PIPE_STDOUT = 1
const val PIPE_EXIT = 2

/**
 * Allows stdout/stderr to be redirected across an
 * ObjectOutputStream.
 */
class RemoteStdio(private val write : ObjectOutputStream) {

    /**
     * Write a line to stderr to be teleported to remote client.
     */
    fun stderr(message : String) {
        write.writeByte(PIPE_STDERR)
        write.writeUTF(message)
    }

    /**
     * Write a line to stdout to be teleported to remote client.
     */
    fun stdout(message : String) {
        write.writeByte(PIPE_STDOUT)
        write.writeUTF(message)
    }

    /**
     * Signal exit. The result code needs to be transported some other
     * way.
     */
    fun exit() {
        write.writeByte(PIPE_EXIT)
    }
}

/**
 * Read an object stream written by RemoteStdio and redirect it
 * to local stdout and stderr.
 */
fun teleportStdio(objectRead: ObjectInputStream, stdio : (Boolean, String) -> Unit) {
    while(true) {
        when(0 + objectRead.readByte()) {
            PIPE_STDERR -> {
                val line = objectRead.readUTF()
                stdio(true, line)
            }
            PIPE_STDOUT -> {
                val line = objectRead.readUTF()
                stdio(false, line)
            }
            PIPE_EXIT -> {
                return
            }
        }
    }
}