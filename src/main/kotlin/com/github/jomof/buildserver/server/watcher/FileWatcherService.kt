package com.github.jomof.buildserver.server.watcher

import com.github.jomof.buildserver.common.DataStorageFolder
import com.github.jomof.buildserver.common.FileWatcherFolder
import com.github.jomof.buildserver.common.RAPTOR_CAGE_BASE_FOLDER
import com.github.jomof.buildserver.common.ServerName
import org.picocontainer.DefaultPicoContainer
import org.picocontainer.behaviors.Caching
import java.io.File
import java.lang.RuntimeException
import java.nio.file.*

interface FileChangeListener {
    fun events(events : List<WatchEvent<*>>)
}

interface FileWatcherService {
    fun addWatchFolder(folder: File)
    fun isWatching(): Boolean
    fun poll()
}

val EVENT_DISCOVERY = object : WatchEvent.Kind<Path> {
    override fun type() = Path::class.java
    override fun name() = "EVENT_DISCOVERY"
}

class FolderWatchKeyListener(
        private val services : Array<FileChangeListener>) {

    fun events(events: List<WatchEvent<*>>) {
        services.forEach { it.events(events) }
    }
}

class DefaultFileWatcherService(
        private val serverName : ServerName) : FileWatcherService {
    private val watched = mutableMapOf<File, WatchFolder>()
    private val watcher = FileSystems.getDefault().newWatchService()
    private val directoryServices = mutableMapOf<File, FolderWatchKeyListener>()

    override fun isWatching() = !watched.isEmpty()

    override fun addWatchFolder(folder: File) {
        if (watched.contains(folder)) return
        synchronized(this) {
            if (!watched.contains(folder)) {
                try {
                    watched[folder] = WatchFolder(watcher, folder.toPath())
                    val pico = DefaultPicoContainer(Caching())
                    pico.addComponent(NinjaWatchDirectoryService::class.java)
                    pico.addComponent(LoggingWatchDirectoryService::class.java)
                    pico.addComponent(FileWatcherFolder(folder))
                    pico.addComponent(DataStorageFolder(
                            File(File(folder, RAPTOR_CAGE_BASE_FOLDER), serverName.name)))
                    pico.addComponent(FolderWatchKeyListener::class.java)
                    pico.addComponent(pico)

                    val folderWatcher = pico.getComponent(FolderWatchKeyListener::class.java)
                    directoryServices[folder] = folderWatcher
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
                    val isOurs = path.toString().contains(RAPTOR_CAGE_BASE_FOLDER)
                    !isOurs
                }
                if (events.isEmpty()) continue
                println("File watcher sees ${events.size} events")
                directoryServices[folder]!!.events(events)
            }
        }
    }
}