package com.github.jomof.buildserver.common.process

import java.io.BufferedReader
import java.io.InputStreamReader

fun Process.waitFor(stdio : (Boolean, String) -> Unit) : Int {
    val inputStream = BufferedReader(InputStreamReader(inputStream!!))
    val errorStream = BufferedReader(InputStreamReader(errorStream!!))

    Thread {
        inputStream.forEachLine {
            synchronized(this) {
                stdio(false, it)
            }
        }
    }.start()

    Thread {
        errorStream.forEachLine {
            synchronized(this) {
                stdio(true, it)
            }
        }
    }.start()

    return waitFor()


}