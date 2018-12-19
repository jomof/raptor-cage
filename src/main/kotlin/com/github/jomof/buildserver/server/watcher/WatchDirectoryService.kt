package com.github.jomof.buildserver.server.watcher

import java.io.File

/**
 * Things that exist once per watched directory.
 */
interface WatchDirectoryService : FileChangeListener
typealias WatchDirectoryServiceFactory = Pair<String, (File, File) -> WatchDirectoryService>

fun getWatchDirectoryServiceFactories(): List<WatchDirectoryServiceFactory> {
    return listOf(
            Pair("log", { watched, storage -> LoggingWatchDirectoryService(watched, storage) } ),
            Pair("ninja", { watched, storage -> NinjaWatchDirectoryService(watched, storage) } )
    )
}

