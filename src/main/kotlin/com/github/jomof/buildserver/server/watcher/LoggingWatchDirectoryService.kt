package com.github.jomof.buildserver.server.watcher

import com.github.jomof.buildserver.common.DataStorageFolder
import com.github.jomof.buildserver.common.FileWatcherFolder
import java.io.File
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent

class LoggingWatchDirectoryService(baseStorage: DataStorageFolder) : FileChangeListener {
    private var discovered = 0
    private var deleted = 0
    private var created = 0
    private var modified = 0
    private var lastDiscovered : String? = "none"
    private var lastDeleted : String? = "none"
    private var lastCreated : String? = "none"
    private var lastModified : String? = "none"
    private val storage = File(baseStorage.folder, "log")
    private val counters = File(storage, "counters.txt")

    init {
        storage.mkdirs()
    }
    override fun events(events: List<WatchEvent<*>>) {
        storage.mkdirs()
        for (event in events) {
            println("Saw file event ${event.kind().name()} ${event.context()}")
            when (event.kind()) {
                EVENT_DISCOVERY -> {
                    discovered += event.count()
                    lastDiscovered = event.context().toString().replace(File.separator, "/")
                }
                StandardWatchEventKinds.ENTRY_CREATE -> {
                    created += event.count()
                    lastCreated = event.context().toString().replace(File.separator, "/")
                }
                StandardWatchEventKinds.ENTRY_DELETE -> {
                    deleted += event.count()
                    lastDeleted = event.context().toString().replace(File.separator, "/")
                }
                StandardWatchEventKinds.ENTRY_MODIFY -> {
                    modified += event.count()
                    lastModified = event.context().toString().replace(File.separator, "/")
                }
            }
        }
        counters.writeText("""
            discovered = $discovered
            last_discovered = $lastDiscovered
            created = $created
            last_created = $lastCreated
            deleted = $deleted
            last_deleted = $lastDeleted
            modified = $modified
            last_modified = $lastModified
        """.trimIndent())
    }

}