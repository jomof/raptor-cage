package com.github.jomof.buildserver.server.store

import com.github.jomof.buildserver.common.localCacheStoreRoot
import com.github.jomof.buildserver.server.store.StoreHandleState.*
import java.io.*
import java.lang.RuntimeException
import java.util.*

/**
 * Hashtable layout:
 *      store/<keyspace>/ht/<hash>/1/key.value
 *      store/<keyspace>/ht/<hash>/1/folder.txt
 *      store/<keyspace>/ht/<hash>/2/key.value
 *      store/<keyspace>/ht/<hash>/2/folder.txt
 *      etc
 *
 * Storage layout (pointed to by folder.txt)
 *      store/<keyspace>/st/<hash>/<random>
 *
 */
data class StoreHandle(
        val serverName : String,
        val keySpace : String,
        val key : Any) {
    var state = OPEN
    val root = File(localCacheStoreRoot(serverName), keySpace)
    val hash = key.hashCode().toString(36)
    val ht = File(root, "ht/$hash")
    val st = File(root, "st/$hash")
    var hashSlot : File? = null
    var store : File? = null

    fun writeable() : File {
        return when(state) {
            OPEN -> {
                state = WRITEABLE
                store = createUniqueFolder(st)
                store!!
            }
            WRITEABLE -> store!!
            READABLE -> throw RuntimeException("StoreHandle is readable")
            COMMITED -> throw RuntimeException("StoreHandle was already committed")
        }
    }

    fun readable() : File? {
        return when(state) {
            OPEN -> {
                val found = findHashSlot(ht, key)
                if (found != null) {
                    state = READABLE
                    hashSlot = found
                    store = storeFolder(found)
                }
                return store
            }
            READABLE -> store!!
            WRITEABLE -> throw RuntimeException("StoreHandle is readable")
            COMMITED -> throw RuntimeException("StoreHandle was already committed")
        }
    }

    fun commit() {
        when(state) {
            WRITEABLE -> {
                var found = findHashSlot(ht, key)
                if (found == null) {
                    found = createUniqueFolder(ht)
                    val keyValue = File(found, "key.store")
                    ObjectOutputStream(DataOutputStream(FileOutputStream(keyValue))).writeObject(key)
                }
                val folderTxt = File(found, "folder.txt")
                folderTxt.writeText(store!!.path) // <-- this is the commit
                state = COMMITED
            }
            else -> throw RuntimeException("Can only commit writeable")
        }
    }

    companion object {
        private fun findHashSlot(ht : File, key : Any): File? {
            val subs = ht.listFiles()
            if (subs != null) {
                for (sub in subs) {
                    val keyValue = File(sub, "key.store")
                    if (keyValue.isFile) {
                        val read = ObjectInputStream(DataInputStream(FileInputStream(keyValue))).readObject()!!
                        if (key == read) {
                            val store = storeFolder(sub)
                            if (store != null) {
                                return sub
                            }
                        }
                    }
                }
            }
            return null
        }

        private fun storeFolder(hashSlot : File) : File? {
            val folderTxt = File(hashSlot, "folder.txt")
            if (folderTxt.isFile) {
                val store = File(folderTxt.readText())
                if (store.isDirectory) {
                    return store
                }
            }
            return null
        }

        private fun createUniqueFolder(sub : File) : File? {
            while(true) {
                val folder = File(sub, Random().nextInt().toString(36))
                if (!folder.exists()) {
                    folder.mkdirs()
                    return folder
                }
            }
        }
    }
}