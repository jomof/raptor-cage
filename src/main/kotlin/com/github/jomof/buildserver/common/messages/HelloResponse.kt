package com.github.jomof.buildserver.common.messages

import java.io.Serializable

data class HelloResponse(
        val type : String = "hello-response",
        val version : Int,
        val buildTime : Long) : Serializable {
    companion object {
        @JvmStatic private val serialVersionUID: Long = 1
    }
}