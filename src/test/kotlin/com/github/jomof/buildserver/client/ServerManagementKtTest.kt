package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.client.getOrStartServer
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ServerManagementKtTest {
    @Test
    fun testGetOrStartServer() {
        val connection = getOrStartServer("testGetOrStartServer")
        assertThat(connection.version()).isEqualTo(1)
        connection.stop()
    }
}