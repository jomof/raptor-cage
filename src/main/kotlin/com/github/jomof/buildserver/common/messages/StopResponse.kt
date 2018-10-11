package com.github.jomof.buildserver.common.messages

import java.io.Serializable

data class StopResponse(
        val type : String = "stop-response") : Serializable {
    companion object {
        @JvmStatic private val serialVersionUID: Long = 1
    }
}