package com.github.jomof.buildserver.common.flags

enum class ClangOperation {
    CC_TO_O,
    C_TO_O,
    CC_TO_II,
    C_TO_I,
    UNKNOWN;

    fun isObjectOutput() : Boolean {
        return this == CC_TO_O || this == C_TO_O
    }
}