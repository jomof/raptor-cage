package com.github.jomof

import com.github.jomof.buildserver.client.getOrStartServer
import org.junit.Test
import java.io.File

class VsCodeOps {

    @Test
    fun vscodeStart() {
        val folder = File("C:\\Users\\Jomo\\projects\\vscode_workspace\\project")
        if (folder.exists()) {
            val connection = getOrStartServer("vscode")
            connection.watch(folder.path)
        }
    }

    @Test
    fun vscodeStop() {
        try {
            val connection = getOrStartServer("vscode")
            connection.stop()
        } catch (e: Throwable) {
        }
    }
}