package com.github.jomof.buildserver

import com.github.jomof.buildserver.common.os
import java.io.*
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

fun localNdkPath(ndk : String) : File {
    val home = File(System.getProperty("user.home"))
    return File(home, ".test/ndks/android-ndk-$ndk")
}

fun localNdkSourceProperties(ndk : String) : File {
    return File(localNdkPath(ndk), "source.properties")
}

fun localNdkDownloadZip(ndk : String) : File {
    val home = File(System.getProperty("user.home"))
    return File(home, ".test/ndks/$ndk.zip")
}

private fun tryGetUrlInputStream(url: URL): InputStream? {
    val con = url.openConnection()
    con.connect()
    return con.getInputStream()
}

private fun copyInputStreamToLocalFile(input: InputStream, localFile: File) {
    val buffer = ByteArray(4096)
    var n: Int

    val output = FileOutputStream(localFile)
    n = input.read(buffer)
    while (n != -1) {
        output.write(buffer, 0, n)
        n = input.read(buffer)
    }
    output.close()
}

fun remoteNdkPath(ndk : String) : URL {
    return URL("https://dl.google.com/android/repository/android-ndk-$ndk-${os.tag}-x86_64.zip");
}

fun unzip(localArchive: File, localUnzipFolder: File) {
    val zipFile = ZipFile(localArchive.path)
    val enu = zipFile.entries()
    while (enu.hasMoreElements()) {
        val zipEntry = enu.nextElement() as ZipEntry

        val name = zipEntry.name
        //            long size = zipEntry.getSize();
        //            long compressedSize = zipEntry.getCompressedSize();
        //            System.out.printf("name: %-20s | size: %6d | compressed size: %6d\n",
        //                name, size, compressedSize);

        val file = File(localUnzipFolder, name)
        if (name.endsWith("/")) {

            file.mkdirs()
            continue
        }

        val parent = file.parentFile
        parent?.mkdirs()

        val `is` = zipFile.getInputStream(zipEntry)
        val fos = FileOutputStream(file)
        val bytes = ByteArray(1024)
        var length = `is`.read(bytes)
        while (length >= 0) {
            fos.write(bytes, 0, length)
            length = `is`.read(bytes)
        }
        `is`.close()
        fos.close()
    }
    zipFile.close()
}

fun getNdkDownloadIfNecessary(ndk : String) : File {
    val path = localNdkPath(ndk)
    val sourceProperties = localNdkSourceProperties(ndk)
    if (!sourceProperties.isFile) {
        path.mkdirs()
        val downloadTo = localNdkDownloadZip(ndk)
        if (!downloadTo.isFile) {
            val url = remoteNdkPath(ndk)
            System.err.println("Downloading $url to $downloadTo")
            val stream = tryGetUrlInputStream(url)!!
            copyInputStreamToLocalFile(stream, downloadTo)
        }
        unzip(downloadTo, path.parentFile)
    }
    return path
}