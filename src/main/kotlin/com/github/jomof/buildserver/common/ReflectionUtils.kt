package com.github.jomof.buildserver.common

import java.io.File
import kotlin.reflect.KClass

fun tryJarUrlToFile(path: String): File? {
    if (path.startsWith("jar:")) {
        val index = path.indexOf("!/")
        if (index == -1) {
            return null
        }
        return File(path.substring(4, index))
    }
    if (path.startsWith("file:")) {
        return File(path.substring(5))
    }
// TODO: Is this code needed?
//    try {
//        return File(URL(path).toURI())
//    } catch (e : Throwable) {
//        println(e.toString())
//    }
    return null
}

fun jarUrlToFile(url: String): String {
    val result = tryJarUrlToFile(url)
            ?: throw RuntimeException("Invalid URL $url")
    val full = result.absolutePath.replace("\\", "/")
    require(File(full).exists())
    return full
}

fun javaExeFolder() =
        File(System.getProperties().getProperty("java.home"), "bin")

fun javaExeBase() = "java" + os.exe

fun javaExe(): File {
    var java = File(javaExeFolder(), javaExeBase())
    if (!java.isFile) {
        throw RuntimeException("Expected to find java at $java but didn't")
    }
    return java
}

fun <T : Any> KClass<T>.classPath(): String {
    val codeSourceLocation = java.protectionDomain.codeSource.location
    if (codeSourceLocation != null) {
        return jarUrlToFile(codeSourceLocation.toString())
    }
// TODO: Is this code needed?
//    val classSimpleName = simpleName
//    val classCanonicalName = canonicalName
//    val classResource = getResource("$classSimpleName.class")
//    val classResourceUrl = classResource!!.toString()
//    val suffix = classCanonicalName.replace('.', '/') + ".class"
//    require(classResourceUrl.endsWith(suffix))
//    var path = classResourceUrl.substring(0, classResourceUrl.length - suffix.length)
//    if (path.startsWith("jar:")) {
//        path = path.substring(4, path.length - 2)
//    }
//    return jarUrlToFile(URL(path).toString())
    throw RuntimeException("Could not access code source location")
}

