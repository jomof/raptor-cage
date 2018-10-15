package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.common.*
import com.github.jomof.buildserver.common.process.callbackBuilder
import com.github.jomof.buildserver.server.RaptorCageDaemon
import java.io.File
import java.net.ConnectException

private fun startServer(serverName : String): ServerConnection {
    val localCachePath = localCachePath(serverName)
    localCachePath.mkdirs()
    val stdout = File(localCachePath, "server-stdout.txt")
    val stderr = File(localCachePath, "server-stderr.txt")
    val pb = RaptorCageDaemon::class.callbackBuilder()
            .detached()
            .processBuilder("Raptor Cage", localCachePath, serverName)
            .directory(localCachePath)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(stdout))
            .redirectError(ProcessBuilder.Redirect.appendTo(stderr))
    log(serverName, "Starting Raptor Cage server")
    val process = pb.start()


    var i = 0
    while (true) {
        log(serverName, "Spinning while attaching")
        val connection = connectServer(serverName)
        if (connection != null) {
            return connection
        }
        Thread.sleep(1000)
        if (++i > 10) {
            throw RuntimeException("Timeout connecting to server")
        }
    }
}

fun connectServer(serverName : String) : ServerConnection? {
    val localPortAgreementFile = localPortAgreementFile(serverName)
    if (localPortAgreementFile.isFile) {
        val port = localPortAgreementFile.readText().toInt()
        return try {
            val result = ServerConnection(serverName, port)
            result
        } catch (e : ConnectException) {
            null
        }
    }
    return null
}

fun getOrStartServer(serverName: String) : ServerConnection {
    return connectServer(serverName)
            ?: startServer(serverName)
}