package com.github.jomof.buildserver.server.model

import com.github.jomof.buildserver.server.model.ClangFlagType.SOURCE

data class SourceFileFlag(
        val sourceFile : String) : ClangFlag() {
    override val sourceFlags = listOf(sourceFile)
    override val flag = sourceFile
    override val type = SOURCE
}