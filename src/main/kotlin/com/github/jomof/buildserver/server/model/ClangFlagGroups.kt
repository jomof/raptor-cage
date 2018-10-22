package com.github.jomof.buildserver.server.model

import com.github.jomof.buildserver.server.model.ClangFlagKind.ONE_ARG

enum class ClangFlagGroups(val flags : List<ClangFlagType>) {
    ONE_ARG_CLANG_FLAGS(
            ClangFlagType.values().filter { it.kind == ONE_ARG });

    private val shortDashSet = flags.mapNotNull { it.short }.map { "-$it" }
    private val longDashSet = flags.mapNotNull { it.long }.map { "--$it" }
    private val oneArg = flags.filter { it.kind == ONE_ARG }
    private val oneArgShortDashSet = oneArg.mapNotNull { it.short }.map { "-$it" }
    private val oneArgLongDashSet = oneArg.mapNotNull { it.long }.map { "--$it" }
    private val oneArgDashSet = oneArgShortDashSet + oneArgLongDashSet
    private val oneArgEquals = oneArgDashSet.map { "$it=" }

    fun contains(flag : String) =
        shortDashSet.contains(flag) || longDashSet.contains(flag)

    fun isOneArgEquals(flag : String) =
        oneArgEquals.any { flag.startsWith(it) }
}