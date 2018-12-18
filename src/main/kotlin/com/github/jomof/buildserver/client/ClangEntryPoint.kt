package com.github.jomof.buildserver.client

import java.io.File

fun doClang(args : List<String>) {
    log("clang", "Raptor cage intercepted")
    val connection = getOrStartServer("main")
    val code = connection.clang(
            directory = File(".").absolutePath,
            args = args.toList()).code

    log("clang", "Reached the end of clang main")
    System.exit(code)
}