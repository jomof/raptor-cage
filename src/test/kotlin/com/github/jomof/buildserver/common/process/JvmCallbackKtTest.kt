package com.github.jomof.buildserver.common.process

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class JavaCallbackKtTest {
    @Test
    fun simpleCallback() {
        val result =
                JavaCallbackKtTest::class.callbackBuilder()
                        .dependsOn(Truth::class)
                        .processBuilder("Test Title", "9", "arg1", "arg2")
                        .inheritIO()
                        .start()
                        .waitFor()
        assertThat(result).isEqualTo(9)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.out.println("OUT: " + args.joinToString(" "))
            System.err.println("ERR: " + args.joinToString(" "))
            System.exit(args[0].toInt())
        }
    }
}