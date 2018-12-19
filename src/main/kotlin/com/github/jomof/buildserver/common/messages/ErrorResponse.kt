package com.github.jomof.buildserver.common.messages

import java.io.Serializable
import java.lang.Exception

data class ErrorResponse(
        val type : String = "error-response",
        val message : String,
        val exception : Exception? = null) : Serializable {
    companion object {
        @JvmStatic private val serialVersionUID: Long = 1
    }
}