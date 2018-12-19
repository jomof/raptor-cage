package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.common.ServerName
import java.io.File

fun doClang(args : List<String>, serverName : ServerName) {
    log(serverName, "Raptor cage intercepted")
    val connection = getOrStartServer(serverName)
    val code = connection.clang(
            directory = File(".").absolutePath,
            args = args.toList()).code

    log(serverName, "Reached the end of clang main")
    System.exit(code)
}