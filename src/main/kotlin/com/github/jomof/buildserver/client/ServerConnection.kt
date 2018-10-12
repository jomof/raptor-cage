package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.common.messages.ErrorResponse
import com.github.jomof.buildserver.common.messages.HelloRequest
import com.github.jomof.buildserver.common.messages.HelloResponse
import com.github.jomof.buildserver.common.messages.StopRequest
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
            log(serverName, "Sending $request")
            val outToServer = DataOutputStream(clientSocket.getOutputStream())
            val objectWrite = ObjectOutputStream(outToServer)
            objectWrite.writeObject(request)
            log(serverName, "Wrote $request")
            val inFromServer = DataInputStream(clientSocket.getInputStream())
            val objectRead = ObjectInputStream(inFromServer)
            log(serverName, "About to readObject")
            val result = objectRead.readObject()
            log(serverName, "Finished readObject with $result")
            if (result is ErrorResponse) {
                throw RuntimeException(result.message)
            }
            return result
        }

    }

    fun version() = hello.version

    fun hello() : HelloResponse {
        return send(HelloRequest()) as HelloResponse
    }

    fun stop() {
        send(StopRequest())
    }
}