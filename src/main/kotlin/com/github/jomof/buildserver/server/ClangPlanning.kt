package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.server.model.ClangCall
import com.github.jomof.buildserver.server.store.StoreHandle
import java.io.File

interface PlanStep

data class ExecuteClang(
        val workingFolder: File,
        val call : ClangCall) : PlanStep
data class CopyFile(
        val fromFolder : File,
        val fromFile : String,
        val toFolder : File,
        val toFile : String) : PlanStep
data class CommitStore(val storeHandle : StoreHandle) : PlanStep

fun createPlan() : List<PlanStep> = listOf()

fun List<PlanStep>.addClangCall(
        workingFolder : File,
        call : ClangCall) = this + ExecuteClang(workingFolder, call)

fun List<PlanStep>.copyOutputsTo(serverName : String) : List<PlanStep> {
    return flatMap { step ->
        when(step) {
            is ExecuteClang -> {
                val call = step.call
                val storeHandle = StoreHandle(
                        serverName,
                        "compile-flags",
                        call.joinToString())
                val writeable = storeHandle.writeable()
                val redirects = call.outputFiles().flatMap {
                    (_, files) ->
                        files.map {
                            val file = File(it)
                            val workingFolder = step.workingFolder.path
                            if(!file.isAbsolute) {
                                CopyFile(step.workingFolder, it, writeable, it)
                            } else if (it.startsWith(workingFolder)){
                                val toRelative = it.substringAfter(workingFolder).substring(1)
                                CopyFile(
                                        step.workingFolder, toRelative,
                                        writeable, toRelative)
                            } else {
                                throw RuntimeException("Couldn't map $it with working folder $workingFolder to store")
                            }
                        }
                }

                listOf(step) + redirects + CommitStore(storeHandle)
            }
            else -> listOf(step)
        }
    }
}