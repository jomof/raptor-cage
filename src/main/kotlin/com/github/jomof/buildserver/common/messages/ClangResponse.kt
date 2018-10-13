package com.github.jomof.buildserver.common.messages

import java.io.Serializable

data class ClangResponse(
        val type : String = "clang-response",
        val code : Int) : Serializable {
    companion object {
        @JvmStatic private val serialVersionUID: Long = 1
    }
}