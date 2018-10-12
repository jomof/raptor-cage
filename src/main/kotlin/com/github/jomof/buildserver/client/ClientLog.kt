package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.common.localCacheClientLogFile

fun log(serverName : String, message : String) {
    val log = localCacheClientLogFile(serverName)
    log.parentFile.mkdirs()
    log.appendText("$message\r\n")
}
