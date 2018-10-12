package com.github.jomof.buildserver.client

/**
 * Document me
 */
class ClangEntryPoint {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("Raptor cage y'all")
            val connection = getOrStartServer("main")
            println("didit")
            log("main", "Starting process")
            val result =
                ProcessBuilder(args.toList())
                    .inheritIO()
                    .start()
                    .waitFor()
            log("main", "Process exited with $result")
            System.exit(result)
        }
    }
}