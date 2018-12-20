package com.github.jomof.buildserver.server.watcher
import java.io.File
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*

class WatchFolder(private val watcher : WatchService, val rootPath: Path) {
    private val keys = mutableMapOf<WatchKey, Path>()
    private val discoveryEvents = mutableListOf<WatchEvent<Path>>()

    init {
        discoveryEvents.addAll(registerAll(rootPath))
    }

    /**
     * Register the given directory with the WatchService
     */
    private fun register(dir: Path) {
        val key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        keys[key] = dir
    }

    private fun registerAll(start: Path) : List<WatchEvent<Path>> {
        return start.toFile().absoluteFile.walkTopDown()
                .filter {
                    if (it.isDirectory) {
                        register(it.toPath())
                        false
                    } else {
                        true
                    }
                }
                .map { file ->
                    pathRelativeToWatchFolder(file)
                }.mapNotNull {
                    object : WatchEvent<Path> {
                        override fun count() = 1
                        override fun kind() = EVENT_DISCOVERY
                        override fun context() = it
                    }
                }.toList()
    }

    fun pollEvents(): List<WatchEvent<*>> {
        val allEvents = mutableListOf<WatchEvent<*>>()
        allEvents.addAll(discoveryEvents)
        discoveryEvents.clear()
        var eventsSeen : Int
        do {
            eventsSeen = 0
            val addedDirectories = mutableListOf<Path>()
            for (key in keys.keys) {
                val events = key.pollEvents()
                val dir = keys[key]!!

                for (event in events) {
                    val kind = event.kind()
                    val name = event.context() as Path
                    val child = dir.resolve(name)
                    if (kind === ENTRY_CREATE) {
                        if (child.toFile().isDirectory) {
                            addedDirectories.add(child)
                        }
                    }
                    if (!child.toFile().isDirectory) {
                        allEvents.add(object : WatchEvent<Path> {
                            override fun count() = event.count()
                            override fun kind() = kind as WatchEvent.Kind<Path>
                            override fun context() = pathRelativeToWatchFolder(child.toFile())
                        })
                    }
                }

                val valid = key.reset()
                if (!valid) {
                    keys.remove(key)
                }
            }
            addedDirectories.onEach {
                allEvents.addAll(registerAll(it))
            }
        } while(eventsSeen > 0)
        return allEvents
    }

    private fun pathRelativeToWatchFolder(file : File) : Path {
        val rootAbsolute = rootPath.toFile().absoluteFile
        val folderNameLength = rootAbsolute.toString().length
        val path = file.absolutePath.substring(folderNameLength)
        val trimmed = path.trimStart(File.separatorChar)
        return File(trimmed).toPath()
    }
}