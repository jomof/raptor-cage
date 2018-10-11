package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.common.*
import com.github.jomof.buildserver.server.ServerOperation
import java.net.ConnectException
import java.util.ArrayList
import kotlin.jvm.internal.Intrinsics

private fun startServer(serverName : String): ServerConnection {

    val result = ArrayList<String>()
    result.add(platformQuote(javaExe()))
    result.add("-classpath")
    val serverClass = ServerOperation::class
    val jarFile = getJarOfClass(ServerOperation::class.java)
    var classPath = jarFile.absolutePath.replace("\\", "/")
    if (!classPath.endsWith(".jar")) {
        val separator = if (isWindows()) ";" else ":"
        classPath = (getJarOfClass(Intrinsics::class.java).absolutePath.replace("\\", "/")
                + separator + classPath)
    }
    result.add(platformQuote(classPath))
    result.add(serverClass.java.canonicalName)
    result.add(serverName)

    val localCachePath = localCachePath(serverName)
    localCachePath.mkdirs()
    println("dir=$localCachePath")
    val pb = ProcessBuilder(result)
            .directory(localCachePath)
            .inheritIO()
    val process = pb.start()

    while (true) {
        if (!process.isAlive) {
            throw RuntimeException("Could not start build server")
        }
        val connection = tryConnectServer(serverName)
        if (connection != null) {
            return connection
        }
        println("Waiting for server")
        Thread.sleep(1000)
    }
}

fun tryConnectServer(serverName : String) : ServerConnection? {
    val localPortAgreementFile = localPortAgreementFile(serverName)
    if (localPortAgreementFile.isFile) {
        val port = localPortAgreementFile.readText().toInt()
        val connection = ServerConnection(port)
        try {
            connection.hello()
        } catch (e : ConnectException) {
            return null
        }
        return connection
    }
    return null
}

fun getOrStartServer(serverName: String) : ServerConnection {
    val connection = tryConnectServer(serverName)
    if (connection != null) {
        return connection
    }
    startServer(serverName)
    return getOrStartServer(serverName)
}