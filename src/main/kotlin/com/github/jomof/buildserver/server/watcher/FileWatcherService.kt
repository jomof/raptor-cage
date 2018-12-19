package com.github.jomof.buildserver.server.watcher

import com.sun.nio.file.ExtendedWatchEventModifier
import java.io.File
import java.nio.file.*

interface FileChangeListener {
    fun events(events : List<WatchEvent<*>>)
}

interface FileWatcherService {
    fun addWatchFolder(folder: File)
    fun isWatching(): Boolean
    fun poll()
}

private var service: FileWatcherService = DefaultFileWatcherService()

fun getFileWatcherService() = service

val EVENT_DISCOVERY = object : WatchEvent.Kind<Path> {
    override fun type() = Path::class.java
    override fun name() = "EVENT_DISCOVERY"
}

class DefaultFileWatcherService : FileWatcherService {
    private val watched = mutableMapOf<File, WatchKey>()
    private val watcher = FileSystems.getDefault().newWatchService()
    private val directoryServices = mutableMapOf<File, List<WatchDirectoryService>>()

    override fun isWatching() = !watched.isEmpty()

    override fun addWatchFolder(folder: File) {
        synchronized(this) {
            if (!watched.contains(folder)) {
                try {
                    watched[folder] = folder.toPath().register(
                            watcher,
                            arrayOf(
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_DELETE,
                                StandardWatchEventKinds.ENTRY_MODIFY),
                            ExtendedWatchEventModifier.FILE_TREE)

                    directoryServices[folder] = getWatchDirectoryServiceFactories().map { (key, factory) ->
                        factory(folder, File(storage(folder), key))
                    }

                    // Now discover all of the files in the path, do it in batches to limit the size in memory
                    // of the list of files
                    val folderNameLength = folder.path.length
                    println("Discovering folder $folder")
                    val start = System.currentTimeMillis()
                    folder.walk()
                    .filter { it.isFile }
                    .filter { !it.path.contains(".raptor_cage") }
                    .map { file ->
                        if (file.isRooted) {
                            val path = file.path.substring(folderNameLength)
                            val trimmed = path.trimStart(File.separatorChar)
                            File(trimmed)
                        } else {
                            file
                        }
                    }
                    .windowed(100, 100, true).forEach { files ->
                        val events = files.map { file -> object : WatchEvent<Path> {
                            override fun count() = 1
                            override fun kind() = EVENT_DISCOVERY
                            override fun context() = file.toPath()
                        }}.toList()
                        for (directoryService in directoryServices[folder]!!) {
                            directoryService.events(events)
                        }
                    }
                    println("Watching $folder, discovery took ${System.currentTimeMillis() - start}ms")
                } catch (e: Throwable) {
                    println("Couldn't watch $folder : $e")
                    throw e
                }
            }
        }
    }

    override fun poll() {
        if (watched.isEmpty()) return
        synchronized(this) {
            for (folder in watched.keys) {
                val watchKey = watched[folder]!!
                val events = watchKey.pollEvents().filter { event ->
                    val path = event.context() as Path
                    val isOurs = path.toString().contains(".raptor_cage")
                    !isOurs
                }
                if (events.isEmpty()) continue
                println("File watcher sees ${events.size} events")
                for (directoryService in directoryServices[folder]!!) {
                    directoryService.events(events)
                }
                // reset the key
                val valid = watchKey.reset()
                if (!valid) {
                    println("Watch key has been unregister")
                }
            }
        }
    }

    private fun storage(watched: File) = File(watched, ".raptor_cage")
}