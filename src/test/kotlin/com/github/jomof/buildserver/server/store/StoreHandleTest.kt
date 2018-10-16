package com.github.jomof.buildserver.server.store

import com.github.jomof.buildserver.common.localCacheStoreRoot
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class StoreHandleTest {

    @Test
    fun simpleTest() {
        localCacheStoreRoot("storeHandleTest").deleteRecursively()
        val handle = StoreHandle("storeHandleTest", "flags", "clang.exe file1.cpp")
        val readable = handle.readable()
        assertThat(readable).isNull()
        val writeable = handle.writeable()
        assertThat(writeable.isDirectory).isTrue()
        File(writeable, "file.txt").writeText("Hello")
        handle.commit()

        val handle2 = StoreHandle("storeHandleTest", "flags", "clang.exe file1.cpp")
        val readable2 = handle2.readable()
        assertThat(readable2).isNotNull()
        println(readable2)
        localCacheStoreRoot("storeHandleTest").deleteRecursively()
    }

    @Test
    fun spam() {
        var failed = false
        fun spin(letter : String) {
            try {
                for (i in 0..100) {
                    if (!failed) {
                        val code = i % 7
                        val handleWrite = StoreHandle("storeHandleSpam", "flags", "key-$code")
                        val writeFile = File(handleWrite.writeable(), "file.txt")
                        //println("thread-$letter writing code $code writing $writeFile")
                        writeFile.writeText("value-$letter-$code")
                        handleWrite.commit()

                        val handleRead = StoreHandle("storeHandleSpam", "flags", "key-$code")
                        val readFolder = handleRead.readable()
                        if (readFolder != null) {
                            //assertThat(readFolder.exists())
                            val readFile = File(handleRead.readable(), "file.txt")
                            //println("thread-$letter reading code $code from $readFile")
                            val text = readFile.readText()
                            assertThat(text).startsWith("value-")
                            assertThat(text).endsWith("-$code")
                        }
                    }
                }
            } catch (e : Exception) {
                failed = true
                throw e
            }
        }

        localCacheStoreRoot("storeHandleSpam").deleteRecursively()

        Thread { spin("a") }.start()
        Thread { spin("b") }.start()
        Thread { spin("c") }.start()
        Thread { spin("d") }.start()
        Thread { spin("e") }.start()
        spin("z")

    }

    @Test
    fun commit() {
    }
}