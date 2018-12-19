package com.github.jomof.buildserver.server.watcher

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.nio.file.FileSystems

class WatchFolderTest {
    @Test
    fun test() {
        val folder = File("./my-watch-folder-test")
        folder.deleteRecursively()
        folder.mkdirs()
        val watcher = FileSystems.getDefault().newWatchService()
        val watchFolder = WatchFolder(watcher, folder.toPath())
        File(folder, "./my-File.txt").writeText("Hello")
        File(folder, "sub").mkdirs()
        assertThat(watchFolder.pollEvents().size).isEqualTo(3)
        assertThat(watchFolder.pollEvents().size).isEqualTo(0)
        File(folder, "./my-dir").mkdirs()
        assertThat(watchFolder.pollEvents().size).isEqualTo(1)
        File(folder, "./my-dir/a/b/c/d").mkdirs()
        assertThat(watchFolder.pollEvents().size).isEqualTo(4)
        folder.deleteRecursively()
    }
}