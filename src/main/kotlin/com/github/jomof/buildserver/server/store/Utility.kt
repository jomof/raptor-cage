package com.github.jomof.buildserver.server.store

import java.io.IOException

fun <T> retry(
        times : Int = 10,
        delay : Long = 100,
        default : T, call : ()->T) : T {
    for (i in (0..times)) {
        try {
            return call()
        } catch (e: IOException) {
            Thread.sleep(delay)
        }
    }
    return default
}