package com.github.jomof

import com.github.jomof.buildserver.client.getOrStartServer
import com.github.jomof.buildserver.common.ServerName
import org.junit.Test
import java.io.File

class VsCodeOps {

    @Test
    fun vscodeStart() {
        val folder = File("C:\\Users\\Jomo\\projects\\vscode_workspace\\project")
        if (folder.exists()) {
            val connection = getOrStartServer(ServerName("vscode"))
            connection.watch(folder.path)
        }
    }

    @Test
    fun vscodeStop() {
        try {
            val connection = getOrStartServer(ServerName("vscode"))
            connection.stop()
        } catch (e: Throwable) {
        }
    }
}