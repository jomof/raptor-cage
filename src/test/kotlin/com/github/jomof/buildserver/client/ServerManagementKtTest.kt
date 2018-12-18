package com.github.jomof.buildserver.client

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ServerManagementKtTest {
    private class ConnectServer(serverName : String) : AutoCloseable {
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
        ConnectServer("testWatch").use { server ->
            val response = server.connection.watch(".")
        }
    }
}