package com.github.jomof.ninja

import com.google.common.truth.Truth.assertThat
import org.junit.Test

import java.io.StringReader

class MakePathsAbsoluteKtTest {

    @Test
    fun simple() {
        val ninja = makePathsAbsolute(parseNinja("X:/a/b/", StringReader(
                "build out//a out//b : cat in/1 in//2")))

        val build = ninja.tops[0] as BuildDef
        assertThat(build.outputs[1].value).isEqualTo("X:/a/b//out//b")
        assertThat(build.inputs[1].value).isEqualTo("X:/a/b//in//2")
    }

    @Test
    fun ruleWorks() {
        val ninja = makePathsAbsolute(parseNinja("/usr/local", StringReader("""
                    rule my_rule
                    build out//a out//b : cat in/1 in//2
                    """.trimIndent())))

        val build = ninja.tops[1] as BuildDef
        assertThat(build.outputs[1].value).isEqualTo("/usr/local/out//b")
        assertThat(build.inputs[1].value).isEqualTo("/usr/local/in//2")
    }


    @Test
    fun rooted() {
        val ninja = makePathsAbsolute(parseNinja("/usr/local", StringReader("""
                    rule my_rule
                    build out//a /usr/local1/out//b : cat in/1 /usr/local1/in//2
                    """.trimIndent())))

        val build = ninja.tops[1] as BuildDef
        assertThat(build.outputs[1].value).isEqualTo("/usr/local1/out//b")
        assertThat(build.inputs[1].value).isEqualTo("/usr/local1/in//2")
    }
}