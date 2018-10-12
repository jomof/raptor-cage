package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.common.flags.ClangFlags
import com.github.jomof.buildserver.common.flags.ClangOperation
import com.github.jomof.buildserver.common.flags.ClangOperation.*

/**
 * Document me
 */
class ClangEntryPoint {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("Raptor cage y'all")
            val connection = getOrStartServer("main")
            val flags = ClangFlags(args.toList())
            if (flags.operation.isObjectOutput()) {
                println("Preprocessing first")
                val objectOutput = flags.lastOutput
                val preprocessorFlags = flags.toPreprocessorEquivalent()
                println("object output = $objectOutput")
                println("preprocessor output = ${preprocessorFlags.lastOutput}")
                executeAndExit(preprocessorFlags.rawFlags.toTypedArray())
            }

            log("main", "Starting process")
            executeAndExit(args)
        }

        private fun executeAndExit(args: Array<String>) {
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