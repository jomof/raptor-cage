package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.common.messages.*
import com.github.jomof.buildserver.server.workitems.NewRequestWorkItem
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class WorkerOperation(
        private val server : ServerOperation) : Runnable {
    override fun run() {
        synchronized(server) {
            server.incrementWorkers()
        }
        try {
            do {
                val workItem = server.popWorkItem() ?: return
                workItem.socket.use { socket ->
                    val inFromClient = DataInputStream(socket.getInputStream())
                    val read = ObjectInputStream(inFromClient)
                    val outToClient = DataOutputStream(socket.getOutputStream())
                    val write = ObjectOutputStream(outToClient)
                    when (workItem) {
                        is NewRequestWorkItem -> {
                            val request = read.readObject()
                            println("got me a $request")
                            when (request) {
                                is HelloRequest -> {
                                    write.writeObject(HelloResponse(version = 1))
                                }
                                is StopRequest -> {
                                    server.stop()
                                    write.writeObject(StopResponse())
                                }
                                else -> {
                                    val error = ErrorResponse(message = "Server did not handle $request")
                                    write.writeObject(error)
                                }
                            }
                        }
                        else -> {
                            val error = ErrorResponse(message = "Server did not handle $workItem")
                            write.writeObject(error)
                        }
                    }
                }
            } while(true)
        } finally {
            server.decrementWorkers()
        }
    }
}