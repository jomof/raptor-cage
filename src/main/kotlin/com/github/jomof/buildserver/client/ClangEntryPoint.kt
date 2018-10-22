package com.github.jomof.buildserver.client

import java.io.File

/**
 * Document me
 */
class ClangEntryPoint {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            log("main", "Raptor cage intercepted")
            val connection = getOrStartServer("main")
            val code = connection.clang(
                    directory = File(".").absolutePath,
                    args = args.toList()).code

            log("main", "Reached the end of main")
        }
    }
}