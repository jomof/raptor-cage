package com.github.jomof.buildserver.ninja

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.StringReader

class NinjaTokenReaderKtTest {
    @Test
    fun skipComments() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            Non-comment
        """.trimIndent()).forEachNonComment { lines += it }
        assertThat(lines).containsExactly("Non-comment")
    }

    @Test
    fun skipCommentsTrims() {
        val lines = mutableListOf<String>()
        StringReader("""
                 # Comment
            Non-comment 1
              Non-comment 2
        """.trimIndent()).forEachNonComment { lines += it }
        assertThat(lines).containsExactly("Non-comment 1", "  Non-comment 2")
    }

    @Test
    fun skipCommentsRemovesBlankLines() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            Non-comment 1

            Non-comment 2
        """.trimIndent()).forEachNonComment { lines += it }
        assertThat(lines).containsExactly("Non-comment 1", "Non-comment 2")
    }

    @Test
    fun continuedLineSimple() {
        val lines = mutableListOf<String>()
        StringReader("""
            Simple
        """.trimIndent()).forEachContinuedLine { lines += it }
        assertThat(lines).containsExactly("Simple")
    }

    @Test
    fun continuedLine() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            Non-$
            comment
        """.trimIndent()).forEachContinuedLine { lines += it }
        assertThat(lines).containsExactly("Non-comment")
    }

    /**
     * https://ninja-build.org/manual.html
     * "Whitespace at the beginning of a line after a line continuation is also stripped."
     */
    @Test
    fun whitespaceAfterContinuation() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            Non-$
              comment $
                indented
        """.trimIndent()).forEachContinuedLine { lines += it }
        assertThat(lines).containsExactly("Non-comment indented")
    }

    @Test
    fun continuedLineBeforeAndAfter() {
        val lines = mutableListOf<String>()
        StringReader("""
            Before
            # Comment
            Non-$
            comment
            After
        """.trimIndent()).forEachContinuedLine { lines += it }
        assertThat(lines).containsExactly("Before", "Non-comment", "After")
    }

    @Test
    fun tokenizeSimpleBuild() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            output.txt : CREATE input.txt
        """.trimIndent()).forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly("output.txt", ":", "CREATE", "input.txt", END_OF_LINE_TOKEN)
    }

    @Test
    fun tokenizeSimpleBuildNoSpaceAfterColon() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            output.txt :CREATE input.txt
        """.trimIndent()).forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly("output.txt", ":", "CREATE", "input.txt", END_OF_LINE_TOKEN)
    }

    @Test
    fun tokenizeSimpleBuildNoSpaceBeforeColon() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            output.txt: CREATE input.txt
        """.trimIndent()).forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly("output.txt", ":", "CREATE", "input.txt", END_OF_LINE_TOKEN)
    }

    @Test
    fun tokenizeSimpleBuildNoSpaceAroundColon() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            output.txt:CREATE input.txt
        """.trimIndent()).forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly("output.txt", ":", "CREATE", "input.txt", END_OF_LINE_TOKEN)
    }

    @Test
    fun tokenizeBuildWithPipe() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            output.txt:CREATE | input1.txt input2.txt
        """.trimIndent()).forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly("output.txt", ":", "CREATE", "|", "input1.txt", "input2.txt", END_OF_LINE_TOKEN)
    }

    @Test
    fun tokenizeIndentBlock() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            output.txt:CREATE input.txt
              PROPERTY = value
        """.trimIndent()).forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly(
                "output.txt", ":", "CREATE", "input.txt", END_OF_LINE_TOKEN,
                INDENT_TOKEN, "PROPERTY", "=", "value", END_OF_LINE_TOKEN)
    }

    @Test
    fun propertyWithNoSpaces() {
        val lines = mutableListOf<String>()
        StringReader("""
          PROPERTY=value
        """.trimIndent()).forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly("PROPERTY", "=", "value", END_OF_LINE_TOKEN)
    }

    @Test
    fun propertyWithSpaces() {
        val lines = mutableListOf<String>()
        StringReader("""
          PROPERTY = value
        """.trimIndent()).forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly("PROPERTY", "=", "value", END_OF_LINE_TOKEN)
    }
}