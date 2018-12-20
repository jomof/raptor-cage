package com.github.jomof.ninja

fun expandIncludes(ninja : NinjaFileDef, include : (String) -> NinjaFile) : NinjaFileDef {
    return expandIncludesNode(ninja, include) as NinjaFileDef
}

private fun expandIncludesNode(node : Node, include : (String) -> NinjaFile) : Node {
    return when(node) {
        is NinjaFileDef -> {
            val newTops : List<Node> = node.tops.flatMap { top ->
                when(top) {
                    is Include -> with(top) {
                        val included = include((file as NinjaFileRef).value)
                        when(included) {
                            is NinjaFileDef -> included.tops
                            is NinjaFileNotFound -> listOf(Include(included))
                            else -> throw RuntimeException("$included")
                        }
                    }
                    is SubNinja -> with(top) {
                        listOf(top.copy(
                                file = include((file as NinjaFileRef).value)))
                    }
                    else -> listOf(top)
                }
            }
            return NinjaFileDef(node.folder, newTops)
        }
        else -> node
    }
}