package com.github.jomof.buildserver.common.messages

import java.io.Serializable

data class HelloRequest(
    val type : String = "hello-request") : Serializable {
    companion object {
        @JvmStatic private val serialVersionUID: Long = 1
    }
}