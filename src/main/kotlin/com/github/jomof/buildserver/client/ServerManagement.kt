package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.common.*
import com.github.jomof.buildserver.server.ServerOperation
import java.net.ConnectException
import java.util.ArrayList
import kotlin.jvm.internal.Intrinsics

private fun startServer(serverName : String): ServerConnection {
    val serverClass = ServerOperation::class
    val jarFile = getJarOfClass(ServerOperation::class.java)
    var classPath = jarFile.absolutePath.replace("\\", "/")
    if (!classPath.endsWith(".jar")) {
        val separator = if (isWindows()) ";" else ":"
        classPath = (getJarOfClass(Intrinsics::class.java).absolutePath.replace("\\", "/")
                + separator + classPath)
    }
    val args = ArrayList<String>()
    args.add("cmd")
    args.add("/c")
    args.add("start")
    args.add("Raptor Cage -- $serverName")
    args.add("/d")
    args.add(javaExeFolder())
    args.add(javaExeBase())
    args.add("-classpath")
    args.add(classPath)
    args.add(serverClass.java.canonicalName)
    args.add(serverName)

    val localCachePath = localCachePath(serverName)
    localCachePath.mkdirs()
    val pb = ProcessBuilder(args)
            .directory(localCachePath)
            .inheritIO()
    pb.start()
    println("Starting Raptor Cage server")
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
    val result = connectServer(serverName)
            ?: startServer(serverName)
    return result
}