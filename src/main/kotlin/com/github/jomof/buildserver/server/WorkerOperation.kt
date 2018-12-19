package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.BuildInfo
import com.github.jomof.buildserver.common.io.RemoteStdio
import com.github.jomof.buildserver.common.messages.*
import com.github.jomof.buildserver.server.watcher.FileWatcherService
import com.github.jomof.buildserver.server.workitems.NewRequestWorkItem
import java.io.*

class WorkerOperation(
        private val server: RaptorCageDaemon,
        private val fileWatcherService : FileWatcherService) : Runnable {
    override fun run() {
        synchronized(server) {
            server.incrementWorkers()
        }
        try {
            do {
                val workItem = server.popWorkItem() ?: return
                synchronized(fileWatcherService) {
                    fileWatcherService.poll()
                    workItem.socket.use { socket ->
                        val inFromClient = DataInputStream(socket.getInputStream())
                        val read = ObjectInputStream(inFromClient)
                        val outToClient = DataOutputStream(socket.getOutputStream())
                        val write = ObjectOutputStream(outToClient)
                        when (workItem) {
                            is NewRequestWorkItem -> {
                                val request = read.readObject()
                                try {
                                    when (request) {
                                        is HelloRequest -> {
                                            println("hello")
                                            write.writeObject(HelloResponse(
                                                    version = serverVersion,
                                                    buildTime = BuildInfo.BUILD_TIME_MILLIS))
                                        }
                                        is ClangRequest -> {
                                            println("Server executing clang")
                                            val code = clang(
                                                    server.serverName,
                                                    request.directory,
                                                    request.args,
                                                    write)
                                            println("Server about to write clang-response")
                                            write.writeObject(ClangResponse(code = code))
                                            println("Server wrote clang-response")
                                        }
                                        is WatchRequest -> {
                                            println("Server starting watch of ${request.directory}")
                                            println("Server about to write watch-response")
                                            try {
                                                fileWatcherService.addWatchFolder(File(request.directory))
                                            } finally {
                                                RemoteStdio(write).exit()
                                            }
                                            write.writeObject(WatchResponse(watching = request.directory))
                                            println("Server wrote watch-response")
                                            fileWatcherService.poll()

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
                                } catch (e: Exception) {
                                    write.writeObject(ErrorResponse(
                                            message = "Exception during $request",
                                            exception = e))
                                }
                            }
                            else -> {
                                val error = ErrorResponse(message = "Server did not handle $workItem")
                                write.writeObject(error)
                            }
                        }
                        write.flush()
                    }
                }
            } while (true)
        } finally {
            server.decrementWorkers()
        }
    }
}