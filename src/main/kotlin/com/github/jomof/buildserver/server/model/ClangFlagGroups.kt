package com.github.jomof.buildserver.server.model

import com.github.jomof.buildserver.server.model.ClangFlagKind.ONE_ARG
import com.github.jomof.buildserver.server.model.ClangFlagKind.PASS_THROUGH_ARG
import com.github.jomof.buildserver.server.model.ClangFlagType.*

enum class ClangFlagGroups(val flags : List<ClangFlagType>) {
    ONE_ARG_CLANG_FLAGS(
            ClangFlagType.values().filter { it.kind == ONE_ARG }),
    PASS_THROUGH_CLANG_FLAGS(
            ClangFlagType.values().filter { it.kind == PASS_THROUGH_ARG }),
    UNUSED_IN_POSTPROCESS_ONLY_PHASE(
            listOf(MD, MF, MT, ISYSTEM, ISYSROOT)),
    UNUSED_IN_PREPROCESS_ONLY_PHASE(
            listOf(WA));

    private val dashMap = flags.flatMap { type ->
        type.dashSet.map { dash -> Pair(dash, type)} }
            .toMap()
    private val oneArg = flags.filter { it.kind == ONE_ARG }
    private val oneArgShortDashSet = oneArg.mapNotNull { it.short }.map { "-$it" }
    private val oneArgLongDashSet = oneArg.mapNotNull { it.long }.map { "--$it" }
    private val oneArgDashSet = oneArgShortDashSet + oneArgLongDashSet
    private val oneArgEquals = oneArgDashSet.map { "$it=" }
    private val prefixSet = flags.flatMap { flag ->
        when(flag.kind) {
            ClangFlagKind.ONE_ARG ->
                flag.dashSet.map { "$it=" }
            ClangFlagKind.PASS_THROUGH_ARG ->
                flag.dashSet.map { "$it," }
            else -> listOf()
        }
    }

    fun contains(flag : String) = dashMap.contains(flag)
    fun contains(flag : ClangFlag) = flags.contains(flag.type)
    fun typeOf(flag : String) = dashMap[flag]!!

    fun split(flag : String) : Pair<String, String>? {
        for (prefix in prefixSet) {
            if (flag.startsWith(prefix)) {
                return Pair(
                        flag.substring(0, prefix.length - 1),
                        flag.substringAfter(prefix))
            }
        }
        return null

    }

    fun canSplit(flag : String) = split(flag) != null
}