package com.github.jomof.buildserver.server.model

data class UnidentifiedClangFlag(val rawFlag : String) : ClangFlag() {
    override val sourceFlags = listOf(rawFlag)
    override val flag = rawFlag
    override fun isFlag(flag : String)  =
            when(rawFlag) {
                "--$flag", "-$flag" ->
                    true
                else ->
                    false
            }
}