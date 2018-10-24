package com.github.jomof.buildserver.server.model

enum class ClangFlagKind {
    BOOLEAN,
    ONE_ARG,
    PASS_THROUGH_ARG,
    SOURCE,
    UNKNOWN
}