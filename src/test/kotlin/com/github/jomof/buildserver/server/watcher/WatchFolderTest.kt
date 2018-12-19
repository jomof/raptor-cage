package com.github.jomof.buildserver.server.watcher

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.lang.RuntimeException
import java.nio.file.FileSystems

class WatchFolderTest {
    fun pollUntil(watchFolder : WatchFolder, expected: Int) : Int {
        var total = 0
        val start = System.currentTimeMillis()
        while (total < expected && (System.currentTimeMillis() - start) < 5000) {
            total += watchFolder.pollEvents().size
            if (total < expected) {
                Thread.sleep(50)
            }
        }
        if (total < expected) {
            throw RuntimeException("timeout after ${System.currentTimeMillis() - start}")
        }
        if (total > expected) {
            throw RuntimeException("Expected $expected, saw $total")
        }
        return total

    }
    @Test
    fun test() {
        val folder = File("./my-watch-folder-test")
        folder.deleteRecursively()
        folder.mkdirs()
        val watcher = FileSystems.getDefault().newWatchService()
        val watchFolder = WatchFolder(watcher, folder.toPath())
        File(folder, "./my-File.txt").writeText("Hello")
        File(folder, "sub").mkdirs()
        pollUntil(watchFolder, 3)
        pollUntil(watchFolder, 0)
        File(folder, "./my-dir").mkdirs()
        pollUntil(watchFolder, 1)
        File(folder, "./my-dir/a/b/c/d").mkdirs()
        pollUntil(watchFolder, 4)
        folder.deleteRecursively()
    }
}