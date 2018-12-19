package com.github.jomof.buildserver.common

import java.io.File

data class ServerName(val name : String)
data class ServerLockFile(val file : File)
data class FileWatcherFolder(val folder : File)
data class DataStorageFolder(val folder : File)