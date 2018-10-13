package com.github.jomof.buildserver.common.messages

import java.io.Serializable

data class ClangRequest(
        val type : String = "clang-request",
        val directory : String,
        val args : List<String>) : Serializable {
    companion object {
        @JvmStatic private val serialVersionUID: Long = 1
    }
}