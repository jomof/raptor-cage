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
    private val separator = if (isWindows()) ";" else ":"
    private var detached = false

    init {
        // Will always need Kotlin intrinsics
        dependsOn(Intrinsics::class)
    }

    fun dependsOn(clazz : KClass<*>) : JvmCallbackBuilder {
        if (!isJar) {
            classPath = clazz.classPath() + separator + classPath
        }
        return this
    }

    fun detached() : JvmCallbackBuilder {
        this.detached = true
        return this
    }

    fun processBuilder(
            title : String,
            vararg processArgs: String) : ProcessBuilder {
        val args = ArrayList<String>()
        if (detached) {
            if (isWindows()) {
                args.add("cmd")
                args.add("/c")
                args.add("start")
                args.add(title)
                args.add("/d")
                args.add(javaExeFolder().path)
                args.add(javaExeBase())
            } else {
                args.add(javaExe().path)
            }
        } else {
            args.add(javaExe().path)
        }
        args.add("-classpath")
        args.add(classPath)
        args.add(entryPoint)
        args.addAll(processArgs)

        if (detached && !isWindows()) {
            args.add("&")
        }

        return ProcessBuilder(args)
    }
}

fun <T : Any> KClass<T>.callbackBuilder() = JvmCallbackBuilder(this)
