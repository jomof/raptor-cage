package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.common.ServerName

fun doWatch(args : List<String>, serverName : ServerName) {
    val connection = getOrStartServer(serverName)
    connection.watch(args[0])
    System.exit(0)
}