package com.github.jomof.buildserver.client

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ServerManagementKtTest {
    @Test
    fun testGetOrStartServer() {
        val connection = getOrStartServer("testGetOrStartServer")
        assertThat(connection.version()).isEqualTo(2)
        connection.stop()
    }
}