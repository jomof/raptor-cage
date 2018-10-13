package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.common.flags.ClangFlags
import com.github.jomof.buildserver.common.messages.ClangResponse
import com.github.jomof.buildserver.common.messages.PIPE_EXIT
import com.github.jomof.buildserver.common.messages.PIPE_STDERR
import com.github.jomof.buildserver.common.messages.PIPE_STDOUT
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.ObjectOutputStream

private fun ObjectOutputStream.stdout(line : String) {
    println(line)
    writeByte(PIPE_STDOUT)
    writeUTF(line)
}

private fun ObjectOutputStream.stderr(line : String) {
    println(line)
    writeByte(PIPE_STDERR)
    writeUTF(line)
}

private fun ObjectOutputStream.exit(code : Int) {
    writeByte(PIPE_EXIT)
    writeObject(ClangResponse(code = code))
}

fun clang(
        directory: String,
        args: List<String>,
        write: ObjectOutputStream) {
    val flags = ClangFlags(args.toList())
    if (flags.operation.isObjectOutput()) {
        val preprocess = flags.toPreprocessEquivalent()
        val postproces = flags.toPostprocessEquivalent()
        write.stdout("Raptor cage writing ${preprocess.lastOutput}")
        execute(directory, preprocess.rawFlags.toTypedArray(), write)
        write.stdout("Raptor cage writing ${postproces.lastOutput}")
        val code = execute(directory, postproces.rawFlags.toTypedArray(), write)
        write.exit(code)
        return
    }
    val code = execute(directory, args.toTypedArray(), write)
    write.exit(code)
    return
}

private fun execute(
        directory: String,
        args: Array<String>,
        write: ObjectOutputStream) : Int {
    val process = ProcessBuilder(args.toList())
            .directory(File(directory))
            .start()
    val inputStream = BufferedReader(InputStreamReader(process.inputStream!!))
    val errorStream = BufferedReader(InputStreamReader(process.errorStream!!))

    Thread {
        inputStream.forEachLine {
            synchronized(write) {
                println("OUT: $it")
                write.stdout(it)
            }
        }
    }.start()

    Thread {
        errorStream.forEachLine {
            synchronized(write) {
                println("ERR: $it")
                write.stderr(it)
            }
        }
    }.start()

    return process.waitFor()
}