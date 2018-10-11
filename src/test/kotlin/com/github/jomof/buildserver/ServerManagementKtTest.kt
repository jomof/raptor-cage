package com.github.jomof.buildserver

import com.github.jomof.buildserver.client.getOrStartServer
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ServerManagementKtTest {
    @Test
    fun testGetOrStartServer() {
        val connection = getOrStartServer("testGetOrStartServer")
        val hello = connection.hello()
        assertThat(hello.type).isEqualTo("hello-response")
        connection.stop()
    }
}