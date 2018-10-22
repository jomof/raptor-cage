package com.github.jomof.buildserver.server.model

data class SourceFileFlag(
        val sourceFile : String) : ClangFlag() {
    override val sourceFlags = listOf(sourceFile)
    override val flag = sourceFile
    override fun isFlag(flag : String) = false
}