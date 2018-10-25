package com.github.jomof.buildserver.server

import com.github.jomof.buildserver.*
import com.github.jomof.buildserver.client.getOrStartServer
import com.github.jomof.buildserver.common.io.teleportStdio
import com.github.jomof.buildserver.common.localCacheStoreRoot
import com.github.jomof.buildserver.server.model.ClangCall
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.*

class MasterServerTest {
    @Test
    fun spam() {
        val serverName = "MasterServerTest-spam"
        localCacheStoreRoot(serverName).deleteRecursively()
        var remaining = 10
        val baseCall = ClangCall(postProcessCppExampleFlags.readLines())
        fun spin(count : Int) {
            val connection = getOrStartServer(serverName)
            val workingFolder = isolatedTestFolder().absoluteFile
            File(workingFolder, "out").mkdirs()
            val args = baseCall
                    .withClangExecutable(clangCompilerToolExample.path)
                    .withSourceInput(postProcessCppExample.path)
                    .withOutput("out/native-lib.cpp.o")
                    .rawFlags
            (0..count).onEach {
                val result = connection.clang(workingFolder.path, args)
                assertThat(result.code).isEqualTo(0)
            }

            --remaining
        }

        (0 until remaining).onEach {
            Thread { spin(10) }.start()
        }

        while(remaining > 0) {
            spin(10)
        }
    }
}