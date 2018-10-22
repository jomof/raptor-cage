package com.github.jomof.buildserver.server.model

data class OneArgFlag(
        val key : String,
        val value : String,
        override val sourceFlags : List<String>) : ClangFlag() {
    override val flag = "$key $value"
    override fun isFlag(flag : String)  =
            when(key) {
                "--$flag", "-$flag" ->
                    true
                else ->
                    false
            }
}