package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.common.io.teleportStdio
import com.github.jomof.buildserver.common.messages.*
import java.io.*
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

    fun serverBuildTime() = hello.buildTime

    fun hello() : HelloResponse {
        return send(HelloRequest()) as HelloResponse
    }

    fun clang(directory : String, args : List<String>) : ClangResponse {
        val request = ClangRequest(
                directory = directory,
                args = args)
        return execute(request, ClangResponse::class.java)
    }

    fun watch(directory : String) : WatchResponse {
        val canonical = File(directory).canonicalFile
        val request = WatchRequest(
                directory = canonical.path
        )
        return execute(request, WatchResponse::class.java)
    }

    private fun <TRequest, TResult> execute(request: TRequest, type : Class<TResult>) : TResult {
        Socket("localhost", port).use { clientSocket ->
            val outToServer = DataOutputStream(clientSocket.getOutputStream())
            val objectWrite = ObjectOutputStream(outToServer)
            objectWrite.writeObject(request)
            val inFromServer = DataInputStream(clientSocket.getInputStream())
            val objectRead = ObjectInputStream(inFromServer)
            teleportStdio(objectRead) { err, message ->
                if (err) {
                    System.err.println(message)
                } else {
                    System.out.println(message)
                }
            }
            val response = objectRead.readObject()
            if (response.javaClass.isAssignableFrom(type)) {
                @Suppress("UNCHECKED_CAST")
                return response as TResult
            }
            when (response) {
                is ErrorResponse -> throw RuntimeException(response.message)
                else -> throw RuntimeException(response.toString())
            }
        }
    }

    fun stop() {
        send(StopRequest())
    }
}