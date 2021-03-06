package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.common.ServerLockFile
import com.github.jomof.buildserver.common.ServerName
import com.github.jomof.buildserver.common.localPortAgreementFile
import com.github.jomof.buildserver.common.localPortAgrementServerLockFile
import com.github.jomof.buildserver.server.watcher.DefaultFileWatcherService
import com.github.jomof.buildserver.server.watcher.FileWatcherService
import com.github.jomof.buildserver.server.watcher.LoggingWatchDirectoryService
import com.github.jomof.buildserver.server.watcher.NinjaWatchDirectoryService
import com.github.jomof.buildserver.server.workitems.NewRequestWorkItem
import com.github.jomof.buildserver.server.workitems.WorkItem
import org.picocontainer.DefaultPicoContainer
import org.picocontainer.behaviors.Caching
import java.io.IOException
import java.io.RandomAccessFile
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

class RaptorCageDaemon(
        val serverName: ServerName,
        private val serverSocket: ServerSocket,
        private val pico : DefaultPicoContainer,
        private val fileWatcherService : FileWatcherService) : Runnable {
    private lateinit var runningThread: Thread
    private var isStopped = false
    private var workersRunning = 0
    private val workItems = mutableListOf<WorkItem>()


    override fun run() {
        try {
            synchronized(this) {
                this.runningThread = Thread.currentThread()
            }
            log(serverName, "Started")
            this.serverSocket.soTimeout = 75
            while (!isStopped()) {
                var clientSocket: Socket? = null
                try {
                    clientSocket = this.serverSocket.accept()
                } catch (e: SocketTimeoutException) {
                    fileWatcherService.poll()
                } catch (e: IOException) {
                    if (isStopped()) {
                        log(serverName, "Server stopped")
                        return
                    }
                    log(serverName, "Error accepting client connection")
                    throw RuntimeException(
                            "Error accepting client connection", e)
                }

                if (clientSocket != null) {
                    // Deserialize the request
                    addWorkItem(NewRequestWorkItem(clientSocket, pico))

                    if (workersRunning < 10) {
                        Thread(pico.getComponent(WorkerOperation::class.java)).start()
                    }
                }
            }
            log(serverName, "Server stopped after loop")
        } finally {
            log(serverName, "Run method exiting")
        }
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
            val pico = DefaultPicoContainer(Caching())
            val serverName = ServerName(args[0])
            val serverLock = ServerLockFile(localPortAgrementServerLockFile(serverName))
            pico.addComponent(serverName)
            pico.addComponent(ServerSocket(0))
            pico.addComponent(serverLock)
            pico.addComponent(pico)
            pico.addComponent(WorkerOperation::class.java)
            pico.addComponent(RaptorCageDaemon::class.java)
            pico.addComponent(DefaultFileWatcherService::class.java)


            RandomAccessFile(serverLock.file, "rw").use { lockFile ->
                lockFile.channel.lock()

                // Fully start the server before publishing the port
                val server = pico.getComponent(RaptorCageDaemon::class.java)
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
                    if (!portAgreementFile.exists()) {
                        log(serverName, "Agreement port disappeared. Stopping.")
                        server.stop()
                        return

                    }
                    val agreementPort = portAgreementFile.readText().toInt()
                    if (agreementPort != server.port()) {
                        log(serverName, "Agreement port changed from ${server.port()} to $agreementPort, stopping server")
                        server.stop()
                        return

                    }
                    Thread.sleep(500)
                } while (server.isStopped)
            }
        }
    }
}
