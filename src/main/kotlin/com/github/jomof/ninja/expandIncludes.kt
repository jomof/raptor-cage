package com.github.jomof.ninja

fun expandIncludes(ninja : NinjaFileDef, include : (String) -> NinjaFileDef) : NinjaFileDef {
    return expandIncludesNode(ninja, include) as NinjaFileDef
}

private fun expandIncludesNode(node : Node, include : (String) -> NinjaFileDef) : Node {
    return when(node) {
        is NinjaFileDef -> {
            val newTops : List<Node> = node.tops.flatMap { top ->
                when(top) {
                    is Include -> include((top.file as NinjaFileRef).value).tops
                    else -> listOf(top)
                }
            }
            return NinjaFileDef(newTops)
        }
        else -> node
    }
}