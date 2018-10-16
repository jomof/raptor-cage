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

fun localCacheClientLogFile(serverName : String) : File {
    return File(localCachePath(serverName), "client-log.txt")
}

fun localCacheServerLogFile(serverName : String) : File {
    return File(localCachePath(serverName), "server-log.txt")
}

fun localCacheStoreRoot(serverName : String) : File {
    return File(localCachePath(serverName), "store")
}