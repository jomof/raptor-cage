package com.github.jomof.buildserver.server.store

enum class StoreHandleState {
    OPEN,
    WRITEABLE,
    READABLE,
    COMMITED
}