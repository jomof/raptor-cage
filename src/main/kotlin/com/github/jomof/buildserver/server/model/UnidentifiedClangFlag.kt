package com.github.jomof.buildserver.server.model

data class UnidentifiedClangFlag(val rawFlag : String) : ClangFlag() {
    override val sourceFlags = listOf(rawFlag)
    override val flag = rawFlag
    override val type = ClangFlagType.UNKNOWN
}
