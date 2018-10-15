package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.common.*
import com.github.jomof.buildserver.common.process.callbackBuilder
import com.github.jomof.buildserver.server.RaptorCageDaemon
import java.io.File
import java.net.ConnectException

private fun startServer(serverName : String): ServerConnection {
    val localCachePath = localCachePath(serverName)
    localCachePath.mkdirs()
    val pb = RaptorCageDaemon::class.callbackBuilder()
            .detached()
            .processBuilder("Raptor Cage", localCachePath, serverName)
            .directory(localCachePath)
            .inheritIO()
    println("Starting Raptor Cage server")
    if(!File(localCachePath, "start-daemon").isFile) {
        throw RuntimeException("xxx")
    }
    val process = pb.start()
    val code = process.waitFor()
    if (code != 0) {
        throw RuntimeException("Shell exited with $code")
    }


    var i = 0
    while (true) {
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