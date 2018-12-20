package com.github.jomof.ninja

import java.io.File

/**
 * Make any paths not absolute into absolute
 */
fun makePathsAbsolute(ninja : NinjaFileDef) : NinjaFileDef {
    return makePathsAbsolute(ninja.folder, ninja as Node) as NinjaFileDef
}

private fun makePathsAbsolute(baseFolder : String, target : String) : String {
    val file = File(target)
    if (file.isRooted) {
        return target
    }
    return "$baseFolder/$target"
}

fun makePathsAbsolute(baseFolder : String, node : Node) : Node {
    return when(node) {
        is NinjaFileDef -> node.copy(
                tops = node.tops.map { makePathsAbsolute(baseFolder, it) })
        is BuildDef -> node.copy(
                explicitOutputs = node.explicitOutputs.map { makePathsAbsolute(baseFolder, it) as BuildRef },
                explicitInputs = node.explicitInputs.map { makePathsAbsolute(baseFolder, it) as BuildRef } )
        is BuildRef -> node.copy(
                value = makePathsAbsolute(baseFolder, node.value),
                original = node)
        else -> node
    }
}