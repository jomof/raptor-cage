package com.github.jomof.buildserver.common.messages

import java.io.Serializable

data class WatchResponse(
        val type : String = "watch-response",
        val watching : String) : Serializable {
    companion object {
        @JvmStatic private val serialVersionUID: Long = 1
    }
}