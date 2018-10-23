package com.github.jomof.buildserver.server.model

import com.github.jomof.buildserver.server.model.ClangFlagKind.BOOLEAN
import com.github.jomof.buildserver.server.model.ClangFlagKind.ONE_ARG

enum class ClangFlags(
        val short : String?,
        val long : String?,
        val kind : ClangFlagKind) {
    PREPROCESS("E", "-preprocess", BOOLEAN),
    ISYSROOT("isysroot", null, ONE_ARG),
    ISYSTEM("isystem", null, ONE_ARG),
    MF("MF", null, ONE_ARG),
    MT("MT", null, ONE_ARG),
    OUTPUT("o", "output", ONE_ARG)
}