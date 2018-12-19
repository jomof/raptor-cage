package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.common.ServerName
import com.github.jomof.buildserver.server.model.ClangCall
import com.github.jomof.buildserver.common.io.RemoteStdio
import com.github.jomof.buildserver.common.process.redirectAndWaitFor
import java.io.File
import java.io.ObjectOutputStream

/**
 * Handles calls to clang on the server.
 */
fun clang(
        serverName: ServerName,
        directory: String,
        args: List<String>,
        write: ObjectOutputStream) : Int {
    val plan = createPlan()
            .addClangCall(File(directory), ClangCall(args.toList()))
            .copyOutputsTo(serverName)

    return clang(plan, write)
}

fun clang(
        plan : List<PlanStep>,
        write: ObjectOutputStream) : Int {
    val stdio = RemoteStdio(write)

    fun execute(folder : File, args: Array<String>) =
            ProcessBuilder(args.toList())
                    .directory(folder)
                    .start()
                    .redirectAndWaitFor(stdio)

    try {
        for (step in plan) {
            when(step) {
                is ExecuteClang -> {
                    val code = execute(
                            step.workingFolder,
                            step.call.rawFlags.toTypedArray())
                    if (code != 0) {
                        return code
                    }
                }
                is CopyFile -> {
                    val from = File(step.fromFolder, step.toFile)
                    val to = File(step.toFolder, step.toFile)
                    //stdio.stdout("Raptor cage copying $from to $to")
                    from.copyTo(to)
                }
                is CommitStore -> {
                    step.storeHandle.commit()
                }
                else -> throw RuntimeException(step.toString())
            }
        }
        return 0
    } finally {
        stdio.exit()
    }
}

