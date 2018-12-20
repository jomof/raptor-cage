package com.github.jomof.ninja

/**
 * Replace // with / everywhere
 */
fun canonicalizeFiles(original : NinjaFileDef) : NinjaFileDef {
   return canonicalizeFilesNode(original) as NinjaFileDef
}
private fun makePathsAbsolute(value : String) = value.replace("//", "/")
private fun canonicalizeFilesNode(node : Node) : Node {
    return when(node) {
        is NinjaFileDef -> node.copy(
                tops = node.tops.map { canonicalizeFilesNode(it) })
        is BuildDef -> node.copy(
            explicitOutputs = node.explicitOutputs.map { canonicalizeFilesNode(it) as BuildRef },
            explicitInputs = node.explicitInputs.map { canonicalizeFilesNode(it) as BuildRef } )
        is BuildRef -> node.copy(value = makePathsAbsolute(node.value))
        else -> node
    }
}