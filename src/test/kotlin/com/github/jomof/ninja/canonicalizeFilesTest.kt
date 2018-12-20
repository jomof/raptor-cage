package com.github.jomof.ninja

import com.google.common.truth.Truth.assertThat
import org.junit.Test

import java.io.StringReader

class CanonicalizeFilesKtTest {

    @Test
    fun canonicalizeFilesTest() {
        val ninja = canonicalizeFiles(parseNinja("/usr/local", StringReader(
                "build out//a out//b : cat in/1 in//2")))

        val build = ninja.tops[0] as BuildDef
        assertThat(build.explicitOutputs[1].value).isEqualTo("out/b")
        assertThat(build.explicitInputs[1].value).isEqualTo("in/2")
    }

    @Test
    fun ruleWorks() {
        val ninja = canonicalizeFiles(parseNinja("/usr/local", StringReader(
                """
                    rule my_rule
                    build out//a out//b : cat in/1 in//2
                    """.trimIndent())))

        val build = ninja.tops[1] as BuildDef
        assertThat(build.explicitOutputs[1].value).isEqualTo("out/b")
        assertThat(build.explicitInputs[1].value).isEqualTo("in/2")
    }
}