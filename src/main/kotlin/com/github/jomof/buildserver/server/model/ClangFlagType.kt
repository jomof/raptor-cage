package com.github.jomof.buildserver.server.model

enum class ClangFlagType(
        val short : String?,
        val long : String?,
        val kind : ClangFlagKind) {
    ISYSROOT("isysroot", null, ClangFlagKind.ONE_ARG),
    ISYSTEM("isystem", null, ClangFlagKind.ONE_ARG),
    MF("MF", null, ClangFlagKind.ONE_ARG),
    MT("MT", null, ClangFlagKind.ONE_ARG),
    OUTPUT("o", "output", ClangFlagKind.ONE_ARG)
}