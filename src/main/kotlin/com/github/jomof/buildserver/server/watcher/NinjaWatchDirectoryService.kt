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
    private val ninjasFile = File(storage, "ninjas.txt")
    private val ninjas = mutableSetOf<Path>()

    init {
        storage.mkdirs()
    }
    override fun events(events: List<WatchEvent<*>>) {
        val originalNinjas = ninjas.map{it}.toSet()
        for (event in events) {
            val path = event.context() as Path
            if (path.toFile().name != "build.ninja") {
                continue
            }
            when (event.kind()) {
                EVENT_DISCOVERY -> {
                    discovered += event.count()
                    lastDiscovered = path.toString().replace(File.separator, "/")
                    ninjas.add(path)
                }
                StandardWatchEventKinds.ENTRY_CREATE -> {
                    created += event.count()
                    lastCreated = path.toString().replace(File.separator, "/")
                    ninjas.add(path)
                }
                StandardWatchEventKinds.ENTRY_DELETE -> {
                    deleted += event.count()
                    lastDeleted = path.toString().replace(File.separator, "/")
                    ninjas.remove(path)
                }
                StandardWatchEventKinds.ENTRY_MODIFY -> {
                    modified += event.count()
                    lastModified = path.toString().replace(File.separator, "/")
                }
            }
            if (ninjas != originalNinjas) {
                println("Ninjas changed")
                ninjasFile.writeText(ninjas.joinToString("\n"));
            } else {
                println("Ninjas didn't change")
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