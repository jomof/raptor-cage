package com.github.jomof.buildserver.common

import java.io.File

fun localCachePath(serverName : ServerName) : File {
    val home = File(System.getProperty("user.home"))
    val buildServer = File(home, RAPTOR_CAGE_BASE_FOLDER)
    return File(buildServer, serverName.name)
}

fun localPortAgreementFile(serverName : ServerName) : File {
    return File(localCachePath(serverName), "port")
}

fun localPortAgrementServerLockFile(serverName : ServerName) : File {
    return File(localCachePath(serverName), "port-server-lock")
}

fun localPortAgrementClientLockFile(serverName : ServerName) : File {
    return File(localCachePath(serverName), "port-client-lock")
}

fun localCacheClientLogFile(serverName : ServerName) : File {
    return File(localCachePath(serverName), "client-log.txt")
}

fun localCacheServerLogFile(serverName : ServerName) : File {
    return File(localCachePath(serverName), "server-log.txt")
}

fun localCacheStoreRoot(serverName : ServerName) : File {
    return File(localCachePath(serverName), "store")
}