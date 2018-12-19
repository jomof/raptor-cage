package com.github.jomof.buildserver.server.watcher

import java.io.File
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchEvent

/**
 * Things that exist once per watched directory.
 */
interface WatchDirectoryService : FileChangeListener
typealias WatchDirectoryServiceFactory = Pair<String, (File, File) -> WatchDirectoryService>

fun getWatchDirectoryServiceFactories(): List<WatchDirectoryServiceFactory> {
    return listOf(
            Pair("log", { watched, storage -> LoggingWatchDirectoryService(watched, storage) })
    )
}

class LoggingWatchDirectoryService(watched: File, storage: File) : WatchDirectoryService {
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
            println("Saw file event ${event.kind()} ${event.context()}")
            when (event.kind()) {
                EVENT_DISCOVERY -> {
                    discovered += event.count()
                    lastDiscovered = event.context().toString().replace(File.separator, "/")
                }
                ENTRY_CREATE -> {
                    created += event.count()
                    lastCreated = event.context().toString().replace(File.separator, "/")
                }
                ENTRY_DELETE -> {
                    deleted += event.count()
                    lastDeleted = event.context().toString().replace(File.separator, "/")
                }
                ENTRY_MODIFY -> {
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