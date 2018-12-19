package com.github.jomof.buildserver.server.watcher

import com.github.jomof.buildserver.common.DataStorageFolder
import com.github.jomof.buildserver.server.utility.removeCommonSegments
import java.io.File
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent

class NinjaWatchDirectoryService(baseStorage: DataStorageFolder) : FileChangeListener {
    private var discovered = 0
    private var deleted = 0
    private var created = 0
    private var modified = 0
    private var lastDiscovered : String? = "none"
    private var lastDeleted : String? = "none"
    private var lastCreated : String? = "none"
    private var lastModified : String? = "none"
    private val storage = File(baseStorage.folder, "ninja")
    private val counters = File(storage, "counters.txt")
    private val ninjasFile = File(storage, "ninjas.txt")
    private val ninjas = mutableSetOf<Path>()
    private val ninjaKeys = mutableMapOf<String, String>()

    init {
        storage.mkdirs()
        println("Ninja storage is at $storage")
    }
    override fun events(events: List<WatchEvent<*>>) {
        storage.mkdirs()
        val newNinjas = ninjas.map{it}.toMutableSet()
        for (event in events) {
            val path = event.context() as Path
            if (path.toFile().name != "build.ninja") {
                continue
            }
            when (event.kind()) {
                EVENT_DISCOVERY -> {
                    discovered += event.count()
                    lastDiscovered = path.toString().replace(File.separator, "/")
                    newNinjas.add(path)
                }
                StandardWatchEventKinds.ENTRY_CREATE -> {
                    created += event.count()
                    lastCreated = path.toString().replace(File.separator, "/")
                    newNinjas.add(path)
                }
                StandardWatchEventKinds.ENTRY_DELETE -> {
                    deleted += event.count()
                    lastDeleted = path.toString().replace(File.separator, "/")
                    newNinjas.remove(path)
                }
                StandardWatchEventKinds.ENTRY_MODIFY -> {
                    modified += event.count()
                    lastModified = path.toString().replace(File.separator, "/")
                }
            }
        }
        updateNinjas(newNinjas)
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

    private fun updateNinjas(newNinjas : Set<Path>) {
        if (ninjas != newNinjas) {
            println("Ninjas changed")
            ninjas.clear()
            ninjas.addAll(newNinjas)
            ninjaKeys.clear()
            ninjaKeys.putAll(ninjas.removeCommonSegments())
            ninjasFile.writeText(ninjaKeys
                    .map { "${it.key} = ${it.value}"}
                    .joinToString("\n"))
        } else {
            println("Ninjas didn't change")
        }
    }
}