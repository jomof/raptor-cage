package com.github.jomof.buildserver.server.utility

import java.nio.file.Path
import java.nio.file.Paths

private fun Collection<Path>.commonElements(): Set<Path> {
    return fold(flatten().toSet()) { current, path ->
        current intersect path
    }
}

private fun Path.removeSegment(remove: Path): Path {
    val filtered = filter { seg -> seg != remove }.map { it.toString() }
    return when {
        filtered.isEmpty() -> Path.of("")
        else -> Paths.get(filtered[0], *filtered.drop(1).toTypedArray())
    }
}

fun Path.toForwardSlashString() : String {
    return joinToString("/")
}

fun Collection<Path>.removeCommonSegments(): Map<String, String> {
    var map = map { Pair(it, it) }.toMap()
    val commonElements = commonElements()
    for (removeSeg in commonElements) {
        val removed: Map<Path, Path> = map
                .map { (seg, path) ->
                    Pair(seg.removeSegment(removeSeg), path)
                }
                .filter { it.first != null }
                .map { Pair(it.first!!, it.second) }
                .toMap()
        if (removed.size == map.size) {
            map = removed
        }
    }
    return map.map { Pair(it.key.toForwardSlashString(), it.value.toForwardSlashString())}.toMap()
}