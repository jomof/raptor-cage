package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.server.utility.removeCommonSegments
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class ServerManagementKtTest {
    private class ConnectServer(serverName: String) : AutoCloseable {
        val connection = getOrStartServer(serverName)
        override fun close() {
            connection.stop()
        }
    }

    @Test
    fun testGetOrStartServer() {
        ConnectServer("testGetOrStartServer").use { server ->
            assertThat(server.connection.version()).isEqualTo(2)
        }
    }

    @Test
    fun testWatch() {
        (0..1).onEach {
            val base = "myTestWatch-$it"
            val testRoot = File("./$base")
            val subFolder = File(testRoot, "sub")
            val createdFile = File(subFolder, "created-file.txt")
            val deletedFile = File(testRoot, "deleted-file.txt")
            val discoveredFile = File(testRoot, "discovered-file.txt")
            val modifiedFile = File(testRoot, "modified-file.txt")
            val countersFile = File(testRoot, ".raptor_cage/log/counters.txt")
            testRoot.deleteRecursively()

            subFolder.mkdirs()

            discoveredFile.writeText("Hello file watcher")
            ConnectServer("$base").use { server ->
                server.connection.watch(testRoot.path)
                deletedFile.writeText("Hello file watcher")
                modifiedFile.writeText("Hello file watcher")
                createdFile.writeText("Hello")
                deletedFile.delete()
                server.connection.watch(testRoot.path) // Force a poll() to make sure modifedFile is ca
                modifiedFile.writeText("Bob")
                server.connection.watch(testRoot.path) // Force a poll() to make sure modifedFile is ca
            }
            val counters = countersFile.readLines()
            assertThat(counters).containsExactly(
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

}

