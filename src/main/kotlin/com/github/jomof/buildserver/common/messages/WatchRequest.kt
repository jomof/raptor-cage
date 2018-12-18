package com.github.jomof.buildserver.common.messages

import java.io.Serializable

data class WatchRequest(
        val type : String = "watch-request",
        val directory : String) : Serializable {
    companion object {
        @JvmStatic private val serialVersionUID: Long = 1
    }
}