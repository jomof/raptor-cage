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
            "ENTRY_MODIFY = my-File.txt",
            "ENTRY_CREATE = sub")

        pollUntil(watchFolder)
        File(folder, "./my-dir").mkdirs()
        pollUntil(watchFolder,
                "ENTRY_CREATE = my-dir")
        File(folder, "./my-dir/a/b/c/d").mkdirs()
        pollUntil(watchFolder,
                "ENTRY_CREATE = my-dir/a",
                "ENTRY_MODIFY = my-dir/a/b",
                "ENTRY_MODIFY = my-dir/a/b/c",
                "ENTRY_MODIFY = my-dir/a"
                )
        folder.deleteRecursively()
    }
}