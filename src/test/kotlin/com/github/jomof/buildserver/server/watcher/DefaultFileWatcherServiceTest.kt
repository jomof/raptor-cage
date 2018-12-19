package com.github.jomof.buildserver.server.watcher

import com.github.jomof.buildserver.client.ServerManagementKtTest
import com.github.jomof.buildserver.common.RAPTOR_CAGE_BASE_FOLDER
import com.github.jomof.buildserver.common.ServerName
import com.google.common.truth.Truth
import org.junit.Test

import org.junit.Assert.*
import java.io.File

class DefaultFileWatcherServiceTest {

    @Test
    fun test() {
        val base = "myDefaultFileWatcherServiceTest"
        val service = DefaultFileWatcherService(ServerName(base))
        val testRoot = File("./$base")
        val subFolder = File(testRoot, "sub")
        val createdFile = File(subFolder, "created-file.txt")
        val deletedFile = File(testRoot, "deleted-file.txt")
        val discoveredFile = File(testRoot, "discovered-file.txt")
        val modifiedFile = File(testRoot, "modified-file.txt")
        val countersFile = File(testRoot, "$RAPTOR_CAGE_BASE_FOLDER/$base/log/counters.txt")
        testRoot.deleteRecursively()
        subFolder.mkdirs()
        discoveredFile.writeText("Hello file watcher")
        service.addWatchFolder(testRoot)
        deletedFile.writeText("Hello file watcher")
        modifiedFile.writeText("Hello file watcher")
        createdFile.writeText("Hello")
        deletedFile.delete()
        service.poll()
        modifiedFile.writeText("Bob")
        service.poll()
        val counters = countersFile.readLines()
        Truth.assertThat(counters).containsExactly(
                "discovered = 1",
                "last_discovered = discovered-file.txt",
                "created = 3",
                "last_created = sub/created-file.txt",
                "deleted = 1",
                "last_deleted = deleted-file.txt",
                "modified = 6",
                "last_modified = modified-file.txt"
        )
        testRoot.deleteRecursively()
    }
}