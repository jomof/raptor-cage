package com.github.jomof.buildserver.common.process

import com.google.common.truth.Truth
import org.junit.Test

import com.google.common.truth.Truth.assertThat

class ProcessUtilsKtTest {
    @Test
    fun simpleWaitForStdio() {
        val sb = StringBuilder()
        val result =
                ProcessUtilsKtTest::class.callbackBuilder()
                        .dependsOn(Truth::class)
                        .processBuilder("Test Title", "9", "arg1", "arg2")
                        .start()
                        .waitFor { err, message ->
                            sb.append("$err=$message,")
                        }
        assertThat(result).isEqualTo(9)
        assertThat(sb.toString()).isEqualTo("false=OUT: 9 arg1 arg2,true=ERR: 9 arg1 arg2,")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.out.println("OUT: " + args.joinToString(" "))
            Thread.sleep(50)
            System.err.println("ERR: " + args.joinToString(" "))
            System.exit(args[0].toInt())
        }
    }
}