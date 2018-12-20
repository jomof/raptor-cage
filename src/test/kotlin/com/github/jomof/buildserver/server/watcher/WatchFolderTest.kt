package com.github.jomof.buildserver.server.watcher

import com.github.jomof.buildserver.server.utility.toForwardSlashString
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.WatchEvent

typealias Event = WatchEvent<*>
typealias EventList = List<Event>

class WatchFolderTest {

    private fun pollUntil(watchFolder : WatchFolder, vararg expect : String)  {
        val events = mutableListOf<Event>()
        val start = System.currentTimeMillis()
        do {
            events += watchFolder.pollEvents()
        } while (events.size < expect.size && (System.currentTimeMillis() - start) < 5000)
        val actual = eventToString(events)
        val expected = expect.toList().toSet()
        assertThat(actual).containsExactlyElementsIn(expected)
    }

    private fun eventToString(events : EventList) : Set<String> {
        return events.map { event ->
            "${event.kind().name()} = ${(event.context() as Path).toForwardSlashString()}"
        }.toSet()
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
        pollUntil(watchFolder,
            "ENTRY_CREATE = my-File.txt",
            "ENTRY_MODIFY = my-File.txt")

        pollUntil(watchFolder)
        File(folder, "./my-dir").mkdirs()
        File(folder, "./my-dir/my-file.txt").writeText("abc")
        pollUntil(watchFolder,
                "EVENT_DISCOVERY = my-dir/my-file.txt")
        File(folder, "./my-dir/a/b/c/d").mkdirs()
        File(folder, "./my-dir/a/b/c/d/file.txt").writeText("xyz")
        pollUntil(watchFolder,
                "EVENT_DISCOVERY = my-dir/a/b/c/d/file.txt"
                )
        folder.deleteRecursively()
    }
}