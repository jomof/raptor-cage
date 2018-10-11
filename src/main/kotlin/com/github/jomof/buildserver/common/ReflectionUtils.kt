package com.github.jomof.buildserver.common

import java.io.File
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL

private fun urlToFile(url: URL): File {
    return urlToFile(url.toString())
}

private fun urlToFile(url: String): File {
    var path = url
    if (path.startsWith("jar:")) {
        val index = path.indexOf("!/")
        path = path.substring(4, index)
    }
    try {
        if (isWindows() && path.matches("file:[A-Za-z]:.*".toRegex())) {
            path = "file:/" + path.substring(5)
        }
        return File(URL(path).toURI())
    } catch (e: MalformedURLException) {
        if (path.startsWith("file:")) {
            path = path.substring(5)
            return File(path)
        }
    } catch (e: URISyntaxException) {
        if (path.startsWith("file:")) {
            path = path.substring(5)
            return File(path)
        }
    }

    throw RuntimeException("Invalid URL $url")
}

fun javaExe(): String {
    var java = (System.getProperties().getProperty("java.home")
            + File.separator + "bin" + File.separator + "java")
    if (isWindows()) {
        java += ".exe"
        java = java.replace("\\", "/")
    }
    val result = File(java)
    if (!result.isFile) {
        throw RuntimeException("Expected to find java at $result but didn't")
    }
    return java
}

fun getJarOfClass(c: Class<*>): File {
    val codeSourceLocation = c.protectionDomain.codeSource.location
    if (codeSourceLocation != null) {
        return urlToFile(codeSourceLocation)
    }

    val classResource = c.getResource(c.simpleName + ".class")
    require(classResource != null)

    assert(classResource != null)
    val url = classResource!!.toString()
    val suffix = c.canonicalName.replace('.', '/') + ".class"
    require(url.endsWith(suffix))

    var path = url.substring(0, url.length - suffix.length)

    if (path.startsWith("jar:")) {
        path = path.substring(4, path.length - 2)
    }

    return urlToFile(URL(path))
}