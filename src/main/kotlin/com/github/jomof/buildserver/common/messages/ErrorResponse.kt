package com.github.jomof.buildserver.common.messages

import java.io.Serializable

data class ErrorResponse(
        val type : String = "error-response",
        val message : String) : Serializable {
    companion object {
        @JvmStatic private val serialVersionUID: Long = 1
    }
}