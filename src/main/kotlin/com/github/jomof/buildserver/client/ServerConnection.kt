package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.common.io.teleportStdio
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
            teleportStdio(objectRead) { err, message ->
                if (err) { System.err.println(message) }
                else { System.out.println(message) }
            }
            println("Client about to read clang-response")
            val result = objectRead.readObject() as ClangResponse
            println("Client read clang-response")
            return result
        }
    }

    fun stop() {
        send(StopRequest())
    }
}