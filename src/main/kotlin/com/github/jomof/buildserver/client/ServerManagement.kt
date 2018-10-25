package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.common.*
import com.github.jomof.buildserver.common.process.callbackBuilder
import com.github.jomof.buildserver.server.RaptorCageDaemon
import java.io.File
import java.io.RandomAccessFile
import java.net.ConnectException
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException

private fun tryLock(lockFile : RandomAccessFile) : FileLock? {
    return try {
        lockFile.channel.tryLock()
    } catch (e : OverlappingFileLockException) {
        null
    }
}

fun getOrStartServer(serverName : String): ServerConnection {
    val lockFile = localPortAgrementClientLockFile(serverName)
    val connection = connectServer(serverName)
    if (connection != null) {
        return connection
    }
    RandomAccessFile(lockFile, "rw").use { lockFile ->
        var lockInfo = tryLock(lockFile)
        while (lockInfo == null) {
            // If couldn't take the lock then spin and try to connect
            Thread.sleep(100)
            val connection = connectServer(serverName)
            if (connection != null) {
                return connection
            }
            lockInfo = tryLock(lockFile)
        }

        // Was able to acquire the lock, start the server
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
        pb.start()
        var i = 0
        while (true) {
            log(serverName, "Spin while attaching")
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
    throw RuntimeException("Unreachable")
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