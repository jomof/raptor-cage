package com.github.jomof.buildserver.server.model

data class OneArgFlag(
        val key : String,
        val value : String,
        override val sourceFlags : List<String>,
        override val type : ClangFlagType) : ClangFlag() {

    override val flag = "$key=$value"
}