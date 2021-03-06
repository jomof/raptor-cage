package com.github.jomof.buildserver.server.model

enum class ClangOperation {
    II_TO_O,
    I_TO_O,
    CC_TO_O,
    C_TO_O,
    CC_TO_II,
    C_TO_I,
    UNKNOWN;

    fun isObjectOutput() : Boolean {
        return this == CC_TO_O || this == C_TO_O
       // return this == II_TO_O || this == I_TO_O || this == CC_TO_O || this == C_TO_O
    }
}