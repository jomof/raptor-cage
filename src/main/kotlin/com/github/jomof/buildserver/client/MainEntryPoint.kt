package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.common.ServerName


class MainEntryPoint {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val serverName = ServerName("main")
            when(args[0]) {
                "clang" -> {
                    doClang(args.asList().drop(1), serverName)
                    return
                }
                "version" -> {
                    doVersion(serverName)
                    return
                }
                else -> {
                    throw RuntimeException("$args")
                }
            }
        }
    }
}