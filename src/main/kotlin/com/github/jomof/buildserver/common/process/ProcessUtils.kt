package com.github.jomof.buildserver.common.process

import com.github.jomof.buildserver.common.io.RemoteStdio
import java.io.BufferedReader
import java.io.InputStreamReader

fun Process.redirectAndWaitFor(stdio: RemoteStdio) : Int {
    return waitFor {
        err, message ->
        with(stdio) {
            if (err) {
                stderr(message)
            } else {
                stdout(message)
            }
        }
    }
}


/**
 * Helper function for Process that redirects stdout and stdin to
 * a callback function. The function will be called once per line
 * and will be synchronized but will not be on the main thread.
 */
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