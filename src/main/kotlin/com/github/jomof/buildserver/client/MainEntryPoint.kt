package com.github.jomof.buildserver.client


class MainEntryPoint {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            when(args[0]) {
                "clang" -> {
                    doClang(args.asList().drop(1))
                    return
                }
                "version" -> {
                    doVersion()
                    return
                }
                else -> {
                    throw RuntimeException("$args")
                }
            }
        }
    }
}