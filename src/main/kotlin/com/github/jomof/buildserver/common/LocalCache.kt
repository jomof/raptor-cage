package com.github.jomof.buildserver.common

import java.io.File

fun localCachePath(serverName : String) : File {
    val home = File(System.getProperty("user.home"))
    val buildServer = File(home, ".raptor-cage")
    return File(buildServer, serverName)
}

fun localPortAgreementFile(serverName : String) : File {
    return File(localCachePath(serverName), "port")
}