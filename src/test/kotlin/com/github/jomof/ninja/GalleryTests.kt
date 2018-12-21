package com.github.jomof.ninja

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.io.StringReader
import java.lang.Exception

class GalleryTests {
    // https://github.com/search?l=Ninja&q=file%3A%22build.ninja%22&type=Code

    private val gallery = File("./ninja/gallery")

    private fun assertIdentical(left : Node, right : Node) {
        //if (left == right) return
        fun error() : Exception {
            return RuntimeException()
        }
        try {
            if (left.javaClass != right.javaClass) {
                throw error()
            }
            when (left) {
                is NinjaFileDef -> {
                    if (right !is NinjaFileDef) throw error()
                    assertThat(left.folder).isEqualTo(right.folder)
                    (left.tops).zip(right.tops).onEach { (l, r) ->
                        assertIdentical(l, r)
                    }
                }
                is BuildDef -> {
                    if (right !is BuildDef) throw error()
                    (left.explicitOutputs).zip(right.explicitOutputs).onEach { (l, r) ->
                        assertIdentical(l, r)
                    }
                    assertIdentical(left.rule, right.rule)
                    (left.explicitInputs).zip(right.explicitInputs).onEach { (l, r) ->
                        assertIdentical(l, r)
                    }
                    (left.properties).zip(right.properties).onEach { (l, r) ->
                        assertIdentical(l, r)
                    }
                }
                is RuleRef -> {
                    if (right !is RuleRef) throw error()
                    if (left.value != right.value) throw error()
                }
                is PoolRef -> {
                    if (right !is PoolRef) throw error()
                    if (left.value != right.value) throw error()
                }
                is BuildRef -> {
                    if (right !is BuildRef) throw error()
                    if (left.value != right.value) throw error()
                }
                is RuleDef -> {
                    if (right !is RuleDef) throw error()
                    assertIdentical(left.name, right.name)
                    (left.properties).zip(right.properties).onEach { (l, r) ->
                        assertIdentical(l, r)
                    }
                }
                is PoolDef -> {
                    if (right !is PoolDef) throw error()
                    assertIdentical(left.name, right.name)
                    (left.properties).zip(right.properties).onEach { (l, r) ->
                        assertIdentical(l, r)
                    }
                }
                is Default -> {
                    if (right !is Default) throw error()
                    (left.file).zip(right.file).onEach { (l, r) ->
                        assertIdentical(l, r)
                    }
                }
                is SubNinja -> {
                    if (right !is SubNinja) throw error()
                    assertIdentical(left.original, right.original)
                }
                is Assignment -> {
                    if (right !is Assignment) throw error()
                    assertIdentical(left.name, right.name)
                    assertIdentical(left.value, right.value)
                }
                is IdentifierRef -> {
                    if (right !is IdentifierRef) throw error()
                    if (left.value != right.value) throw error()
                }
                is UninstantiatedLiteral -> {
                    if (right !is UninstantiatedLiteral) throw error()
                    if (left.value != right.value) throw error()
                }
                is NinjaFileRef -> {
                    if (right !is NinjaFileRef) throw error()
                    if (left.value != right.value) throw error()
                }
                is Include -> {
                    if (right !is Include) throw error()
                    assertIdentical(left.file, right.file)
                }
                else -> throw RuntimeException("$left")
            }
        } catch (e : Throwable) {
            val ls = writeNinjaToString(left).split("\n")[0]
            val rs = writeNinjaToString(right).split("\n")[0]
            if (ls.length < 256 && rs.length < 256) {
                println("Left ${left.javaClass.simpleName}:\n$ls")
                println("Right ${right.javaClass.simpleName}:\n$rs")
            }
            throw e
        }
    }

    @Test
    fun roundTripPhonyTarget() {
        val body = "build cmake_object_order_depends_target_native-lib: phony"
        roundTrip(parseNinja("folder", StringReader(body)))
    }

    @Test
    fun roundTripBuild() {
        val body = "build out: rule in"
        roundTrip(parseNinja("folder", StringReader(body)))
    }

    @Test
    fun roundTripTopLevelAssignment() {
        val body = "a = b"
        roundTrip(parseNinja("folder", StringReader(body)))
    }

    @Test
    fun roundTripColonInBuildOutput() {
        val body = "build build.ninja: RERUN_CMAKE C\$:/abc"
        roundTrip(parseNinja("folder", StringReader(body)))
    }

    @Test
    fun testDollarColon() {
        val body = """
            rule a
              command = python.exe policy${'$'}: --include
        """.trimIndent()
        roundTrip(parseNinja("folder", StringReader(body)))
    }

    @Test
    fun roundTripAll() {
        gallery.walk().filter { it.name.contains("build") &&
                it.name.endsWith(".ninja") }.forEach { buildNinja ->
            try {
                val topNinja = parseNinja(buildNinja) // Doesn't expand includes
                roundTrip(topNinja)
                val ninja = readNinjaFile(buildNinja) // Expands includes
                roundTrip(ninja)
            } catch (e : Exception) {
                println("$buildNinja")
                throw e
            }
        }
    }

    private fun roundTripSingle(ninja: NinjaFileDef) {
        val ninjaBody = writeNinjaToString(ninja)
        try {
            val ninjaBodyNode = parseNinja(ninja.folder, StringReader(ninjaBody))
            assertIdentical(ninja, ninjaBodyNode)
        } catch (e: Throwable) {
            println(ninjaBody)
            throw e
        }
    }

    private fun roundTrip(ninja: NinjaFileDef) {
        try {
            roundTripSingle(ninja)
            roundTripSingle(makePathsAbsolute(ninja))
            roundTripSingle(canonicalizeFiles(ninja))
            roundTripSingle(qualifyRules("build/x86", ninja))
        } catch (e : Exception) {
            println("File: ${ninja.folder}")
            throw e
        }
    }

    @Test
    fun copyFrom() {
        val from = File("C:\\src\\chromium-2").canonicalFile
        if (!from.exists()) return
        val fromLength = from.path.length + 1
        val to = File("./ninja/gallery/chromium").canonicalFile
        if (to.exists()) return
        fun match(file : File) : Boolean {
            return when {
                file.name.endsWith(".ninja") -> true
                else -> false
            }
        }
        from.walk().filter { match(it) }.forEach { source ->
            val postfix = source.path.substring(fromLength)
            val destination = File(to, postfix)
            println("$source -> $destination")
            source.copyTo(destination)
        }
    }
}