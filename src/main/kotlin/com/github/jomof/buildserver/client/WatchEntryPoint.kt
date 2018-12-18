package com.github.jomof.buildserver.client

fun doWatch(args : List<String>) {
    val connection = getOrStartServer("main")
    connection.watch(args[0])
    System.exit(0)
}