package com.github.jomof.ninja

/**
 * Qualify rule names with a prefix
 */
fun qualifyRules(prefix : String, ninja : NinjaFileDef) : NinjaFileDef {
    return qualifyRules(prefix, ninja as Node) as NinjaFileDef
}

private fun qualifyRules(prefix : String, target : String) : String {
    return "$prefix/$target"
}

private fun qualifyRules(prefix : String, node : Node) : Node {
    return when(node) {
        is NinjaFileDef ->
            node.copy(tops = node.tops.map { qualifyRules(prefix, it) })
        is BuildDef ->
            node.copy(rule = qualifyRules(prefix, node.rule) as RuleRef)
        is RuleDef ->
            node.copy(name = qualifyRules(prefix, node.name) as RuleRef)
        is RuleRef ->
            node.copy(value = qualifyRules(prefix, node.value), original = node)
        else -> node
    }
}