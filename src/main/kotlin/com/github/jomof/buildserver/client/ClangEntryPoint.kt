package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.common.flags.ClangFlags

/**
 * Document me
 */
class ClangEntryPoint {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("Raptor cage intercepted")
            val joined = args.joinToString(" ")
            log("main", "\r\nentry point: $joined")
            if (joined.contains("-E")) throw RuntimeException("why?!")
            val connection = getOrStartServer("main")
            val flags = ClangFlags(args.toList())
            if (flags.operation.isObjectOutput()) {
                val preprocess = flags.toPreprocessEquivalent()
                val postproces = flags.toPostprocessEquivalent()
                println("Raptor cage writing ${preprocess.lastOutput}")
                execute(preprocess.rawFlags.toTypedArray())
                println("Raptor cage writing ${postproces.lastOutput}")
                execute(postproces.rawFlags.toTypedArray())
            }
            executeAndExit(args)
        }

        private fun executeAndExit(args: Array<String>) {
            val result = execute(args)
            System.exit(result)
        }

        private fun execute(args: Array<String>): Int {
            return ProcessBuilder(args.toList())
                    .inheritIO()
                    .start()
                    .waitFor()
        }
    }
}