package com.github.jomof.buildserver.common.messages

import java.io.Serializable

data class StopRequest(
        val type : String = "stop-request") : Serializable {
    companion object {
        @JvmStatic private val serialVersionUID: Long = 1
    }
}