package com.github.jomof.buildserver.server.watcher
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*

class WatchFolder(private val watcher : WatchService, val rootPath: Path) {
    private val keys = mutableMapOf<WatchKey, Path>()

    init {
        registerAll(rootPath)
    }

    /**
     * Register the given directory with the WatchService
     */
    private fun register(dir: Path) {
        val key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        keys[key] = dir
    }

    private fun registerAll(start: Path) {
        start.toFile().walkTopDown()
                .forEach {
                    if (it.isDirectory) {
                        register(it.toPath())
                    }
                }
    }

    fun pollEvents(): List<WatchEvent<*>> {
        val allEvents = mutableListOf<WatchEvent<*>>()
        var key = watcher.poll()
        while (key != null) {
            val dir = keys[key]!!
            val events = key.pollEvents()

            for (event in events) {
                val kind = event.kind()
                val name = event.context() as Path
                val child = dir.resolve(name)
                if (kind === ENTRY_CREATE) {
                    if (child.toFile().isDirectory) {
                        registerAll(child)
                    }
                }
                allEvents.add(object : WatchEvent<Path> {
                    override fun count() = event.count()
                    override fun kind() = kind as WatchEvent.Kind<Path>
                    override fun context() = rootPath.relativize(child)
                })
            }

            val valid = key.reset()
            if (!valid) {
                keys.remove(key)
            }
            key = watcher.poll()
        }
        return allEvents
    }

}