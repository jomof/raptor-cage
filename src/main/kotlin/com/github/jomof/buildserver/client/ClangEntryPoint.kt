package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.common.flags.ClangFlags
import java.io.File

/**
 * Document me
 */
class ClangEntryPoint {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("Raptor cage intercepted")
            val connection = getOrStartServer("main")
            System.exit(connection.clang(
                    File(".").absolutePath,
                    args.toList()).code)
        }
    }
}