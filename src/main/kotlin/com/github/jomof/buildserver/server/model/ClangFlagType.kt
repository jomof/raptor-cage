package com.github.jomof.buildserver.server.model

import com.github.jomof.buildserver.server.model.ClangFlagKind.*

enum class ClangFlagType(
        val short : String?,
        val long : String?,
        val kind : ClangFlagKind) {
    PREPROCESS("E", "preprocess", BOOLEAN),
    ISYSROOT("isysroot", null, ONE_ARG),
    ISYSTEM("isystem", null, ONE_ARG),
    MD("MD", null, BOOLEAN),
    MF("MF", null, ONE_ARG),
    MT("MT", null, ONE_ARG),
    OUTPUT("o", "output", ONE_ARG),
    SOURCE(null, null, ClangFlagKind.SOURCE),
    UNKNOWN(null, null, ClangFlagKind.UNKNOWN),
    WA("Wa", null, PASS_THROUGH_ARG);

    val dashSet = dashSet()

    private fun dashSet() : Set<String> {
        val result = mutableSetOf<String>()
        if (short != null) {
            result += "-$short"
        }
        if (long != null) {
            result += "--$long"
        }
        return result
    }
}