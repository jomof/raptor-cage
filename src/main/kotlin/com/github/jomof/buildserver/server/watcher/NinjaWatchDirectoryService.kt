package com.github.jomof.buildserver.server.watcher

import java.io.File
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent

class NinjaWatchDirectoryService(watched: File, storage: File) : WatchDirectoryService {
    private var discovered = 0
    private var deleted = 0
    private var created = 0
    private var modified = 0
    private var lastDiscovered : String? = "none"
    private var lastDeleted : String? = "none"
    private var lastCreated : String? = "none"
    private var lastModified : String? = "none"
    private val counters = File(storage, "counters.txt")

    init {
        println("Logging $watched")
        storage.mkdirs()
    }
    override fun events(events: List<WatchEvent<*>>) {
        for (event in events) {
            val path = event.context() as Path
            if (path.toFile().name != "build.ninja") {
                continue
            }
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