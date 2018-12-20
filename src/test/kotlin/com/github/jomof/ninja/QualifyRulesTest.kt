package com.github.jomof.ninja

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.StringReader

class QualifyRulesTest {
    @Test
    fun test() {
        val ninja = qualifyRules("debug/x86", parseNinja("build.ninja", StringReader("""
                    rule my_rule
                    build out//a out//b : my_rule in/1 in//2
                    """.trimIndent())))

        val rule = ninja.tops[0] as RuleDef
        assertThat(rule.name.value).isEqualTo("debug/x86/my_rule")
        val build = ninja.tops[1] as BuildDef
        assertThat(build.rule.value).isEqualTo("debug/x86/my_rule")
    }
}