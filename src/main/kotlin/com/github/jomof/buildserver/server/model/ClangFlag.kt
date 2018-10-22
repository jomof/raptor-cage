package com.github.jomof.buildserver.server.model

abstract class ClangFlag {
    abstract val sourceFlags : List<String>
    abstract val flag : String
    abstract fun isFlag(flag : String) : Boolean
}