package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.common.io.PIPE_EXIT
import com.github.jomof.buildserver.common.io.PIPE_STDERR
import com.github.jomof.buildserver.common.io.PIPE_STDOUT
import com.github.jomof.buildserver.common.messages.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket

class ServerConnection(
        val serverName : String,
        val port : Int) {
    private val hello : HelloResponse = hello()
    private fun send(request : Any) : Any {
        Socket("localhost", port).use { clientSocket ->
            val outToServer = DataOutputStream(clientSocket.getOutputStream())
            val objectWrite = ObjectOutputStream(outToServer)
            objectWrite.writeObject(request)
            val inFromServer = DataInputStream(clientSocket.getInputStream())
            val objectRead = ObjectInputStream(inFromServer)
            val result = objectRead.readObject()
            if (result is ErrorResponse) {
                throw RuntimeException(result.message)
            }
            return result
        }

    }

    fun version() = hello.version

    private fun hello() : HelloResponse {
        return send(HelloRequest()) as HelloResponse
    }

    fun clang(directory : String, args : List<String>) : ClangResponse {
        val request = ClangRequest(
                directory = directory,
                args = args)
        Socket("localhost", port).use { clientSocket ->
            val outToServer = DataOutputStream(clientSocket.getOutputStream())
            val objectWrite = ObjectOutputStream(outToServer)
            objectWrite.writeObject(request)
            val inFromServer = DataInputStream(clientSocket.getInputStream())
            val objectRead = ObjectInputStream(inFromServer)
            while(true) {
                when(0 + objectRead.readByte()) {
                    PIPE_STDERR -> {
                        val line = objectRead.readUTF()
                        System.err.println(line)
                    }
                    PIPE_STDOUT -> {
                        val line = objectRead.readUTF()
                        System.out.println(line)
                    }
                    PIPE_EXIT -> {
                        return objectRead.readObject() as ClangResponse
                    }
                }
            }
        }
        throw RuntimeException("unreachable")
    }

    fun stop() {
        send(StopRequest())
    }
}