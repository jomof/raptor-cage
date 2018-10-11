package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.server.workitems.NewRequestWorkItem
import com.github.jomof.buildserver.common.localPortAgreementFile
import com.github.jomof.buildserver.server.workitems.WorkItem
import java.net.ServerSocket
import java.io.IOException
import java.net.Socket

class ServerOperation(
        private var serverSocket: ServerSocket) : Runnable {
    private lateinit var runningThread: Thread
    private var isStopped = false
    private var workersRunning = 0
    private val workItems = mutableListOf<WorkItem>()

    override fun run() {
        synchronized(this) {
            this.runningThread = Thread.currentThread()
        }
        while (!isStopped()) {
            var clientSocket: Socket?
            try {
                clientSocket = this.serverSocket.accept()
            } catch (e: IOException) {
                if (isStopped()) {
                    println("server Stopped.")
                    return
                }
                throw RuntimeException(
                        "Error accepting client connection", e)
            }

            // Deserialize the request
            addWorkItem(NewRequestWorkItem(clientSocket))

            if (workersRunning < 10) {
                Thread(WorkerOperation(this)).start()
            }
        }
        println("server Stopped.")
    }

    @Synchronized
    fun popWorkItem(): WorkItem? {
        if (workItems.isEmpty()) {
            return null
        }
        val workItem = workItems[0]
        workItems.removeAt(0)
        return workItem
    }

    @Synchronized
    private fun addWorkItem(workItem: WorkItem) {
        workItems += workItem
    }

    @Synchronized
    private fun isStopped(): Boolean {
        return this.isStopped
    }

    @Synchronized
    fun workers(): Int {
        return workersRunning
    }

    @Synchronized
    fun incrementWorkers() {
        workersRunning++
    }

    @Synchronized
    fun decrementWorkers() {
        workersRunning--
    }

    @Synchronized
    fun stop() {
        this.isStopped = true
        try {
            this.serverSocket.close()
        } catch (e: IOException) {
            throw RuntimeException("Error closing server", e)
        }
    }

    fun port() = serverSocket.localPort

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val serverName = args[0]
            // Fully start the server before publishing the port
            val server = ServerOperation(ServerSocket(0))
            Thread(server).start()
            // At this point, we could serve requests but no one knows
            // our port number.
            val portAgreementFile = localPortAgreementFile(serverName)
            portAgreementFile.writeText(server.port().toString())

            // Now we spin, periodically checking health
            do {
                // If at any point the agreement port changes then we no longer
                // control this cache directory. We have to stop to avoid racing
                // with another server.
                val agreementPort = portAgreementFile.readText().toInt()
                if (agreementPort != server.port()) {
                    println("Agreement port changed from ${server.port()} to $agreementPort, stopping server")
                    server.stop()
                    return

                }
                Thread.sleep(500)
            } while (true)
        }
    }
}
