package com.github.jomof.buildserver.common.process

import com.github.jomof.buildserver.common.*
import java.io.File
import java.util.ArrayList
import kotlin.jvm.internal.Intrinsics
import kotlin.reflect.KClass

class JvmCallbackBuilder(clazz : KClass<*>) {
    private val entryPoint = clazz.java.canonicalName
    private val mainClassPath : String = clazz.classPath()
    private val isJar = mainClassPath.endsWith(".jar")
    private var classPath = mainClassPath
    private var detached = false

    init {
        // Will always need Kotlin intrinsics
        dependsOn(Intrinsics::class)
    }

    fun dependsOn(clazz : KClass<*>) : JvmCallbackBuilder {
        if (!isJar) {
            classPath = clazz.classPath() + os.classPathSeparator + classPath
        }
        return this
    }

    fun detached() : JvmCallbackBuilder {
        this.detached = true
        return this
    }

    fun processBuilder(
            title : String,
            directory : File,
            vararg processArgs: String) : ProcessBuilder {
        val shellArgs = ArrayList<String>()
        val args = ArrayList<String>()
        if (detached) {
            if (os == Os.WINDOWS) {
                shellArgs.add("cmd")
                shellArgs.add("/c")
                shellArgs.add("start")
                shellArgs.add(title)
                shellArgs.add("/d")
                args.add(javaExeFolder().path)
                args.add(javaExeBase())
            } else {
                shellArgs.add("/bin/sh")
                shellArgs.add("-c")
                args.add(javaExe().path)
            }
        } else {
            args.add(javaExe().path)
        }
        args.add("-classpath")
        args.add(classPath)
        args.add(entryPoint)
        args.addAll(processArgs)

        return if (detached) {
            if (os == Os.WINDOWS) {
                ProcessBuilder(shellArgs + args)
            } else {
                val command = File(directory, "start-daemon")
                command.writeText("/bin/sh -c '( ${args.joinToString(" ")} & )\n'")
                command.setExecutable(true)
                ProcessBuilder(command.path)
            }
        } else {

            ProcessBuilder(args)
        }
    }
}

fun <T : Any> KClass<T>.callbackBuilder() = JvmCallbackBuilder(this)
