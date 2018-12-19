package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.common.ServerName
import com.github.jomof.buildserver.common.localCacheServerLogFile

fun log(serverName : ServerName, message : String) {
    val log = localCacheServerLogFile(serverName)
    log.parentFile.mkdirs()
    log.appendText("$message\r\n")
    println(message)
}
