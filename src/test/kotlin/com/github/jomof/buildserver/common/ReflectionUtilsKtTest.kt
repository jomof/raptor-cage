package com.github.jomof.buildserver.common

import com.github.jomof.buildserver.server.ServerOperation
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import net.java.quickcheck.Generator
import net.java.quickcheck.QuickCheck
import net.java.quickcheck.characteristic.AbstractCharacteristic
import net.java.quickcheck.generator.CombinedGenerators.pairs
import net.java.quickcheck.generator.PrimitiveGenerators.strings
import org.junit.Test
import java.io.File

class ReflectionUtilsKtTest {
    class UrlPrefixGenerator : Generator<String> {
        private var i = 0
        private val prefixes = listOf(
                "", "/", "//", "\\", "\\\\",
                "jar", "jar:", "jar:/", "jar://", "jar:Z://",
                "file", "file:", "file:/", "file://", "file:Z://",
                "http", "http:", "http:/", "http://", "http:Z://"
                )
        override fun next(): String {
            return prefixes[(i++) % prefixes.size]
        }
    }

    @Test
    fun jarWithNoBangSlash() {
        assertThat(tryJarUrlToFile("jar:/")).isNull()
    }

    @Test
    fun nonHierarchical() {
        assertThat(tryJarUrlToFile("http:Z://,cjRLX#?")).isNull()
    }

    @Test
    fun jarOfTruth() {
        val jar = File(Truth::class.classPath())
        assertThat(jar.exists()).isTrue()
        assertThat(jar.extension).isEqualTo("jar")
    }

    @Test
    fun jarOfThisTest() {
        val jar = File(ReflectionUtilsKtTest::class.classPath())
        // Test classes go to a directory that is incrementally updated
        // rather than a zipped-up jar
        assertThat(jar.isDirectory).isTrue()
    }

    @Test
    fun jarOfCodeSideClass() {
        val jar = File(ServerOperation::class.classPath())
        println(jar.toString())
        // Test classes go to a directory that is incrementally updated
        // rather than a zipped-up jar
        assertThat(jar.isDirectory).isTrue()
    }

    @Test
    fun fuzzJarUrlToFile() {
        QuickCheck.forAll(
                pairs(UrlPrefixGenerator(), strings()),
                object : AbstractCharacteristic<net.java.quickcheck.collection.Pair<String, String>>() {
                    override fun doSpecify(any: net.java.quickcheck.collection.Pair<String, String>?) {
                        require(any != null)
                        if (any != null) {
                            tryJarUrlToFile(any.first + any.second)
                            tryJarUrlToFile(any.first + "something!/" + any.second)
                        }
                    }
                })
    }
}