package com.github.jomof.buildserver.server.utility

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class PathUtilityKtTest {

    private fun toPaths(s : String) : Set<Path> {
        return s.trimIndent().split("\n").map { it.trim() }.map { File(it).toPath() }.toSet()
    }

    @Test
    fun oneLine() {
        val paths = toPaths("""
            mylibrary/.externalNativeBuild/cmake/debug/arm64-v8a/build.ninja
        """)

        val map = paths.removeCommonSegments()
        assertThat(map).hasSize(paths.size)
        assertThat(map.values).containsExactlyElementsIn(paths.map { it.toForwardSlashString()})
        assertThat(map[""])
                .isEqualTo("mylibrary/.externalNativeBuild/cmake/debug/arm64-v8a/build.ninja")
        println(map)
    }

    @Test
    fun twoLines() {
        val paths = toPaths("""
            mylibrary/.externalNativeBuild/cmake/debug/arm64-v8a/build.ninja
            mylibrary/.externalNativeBuild/cmake/debug/armeabi-v7a/build.ninja
        """)

        val map = paths.removeCommonSegments()
        assertThat(map).hasSize(paths.size)
        assertThat(map.values).containsExactlyElementsIn(paths.map { it.toForwardSlashString()})
        assertThat(map["arm64-v8a"])
                .isEqualTo("mylibrary/.externalNativeBuild/cmake/debug/arm64-v8a/build.ninja")
        println(map)
    }

    @Test
    fun releaseDebug() {
        val paths = toPaths("""
            mylibrary/.externalNativeBuild/cmake/debug/arm64-v8a/build.ninja
            mylibrary/.externalNativeBuild/cmake/release/armeabi-v7a/build.ninja
        """)

        val map = paths.removeCommonSegments()
        assertThat(map).hasSize(paths.size)
        assertThat(map.values).containsExactlyElementsIn(paths.map { it.toForwardSlashString()})
        println(map)
        assertThat(map["debug/arm64-v8a"])
                .isEqualTo("mylibrary/.externalNativeBuild/cmake/debug/arm64-v8a/build.ninja")
    }

    @Test
    fun bigTest() {
        val paths = toPaths("""
            mylibrary/.externalNativeBuild/cmake/debug/arm64-v8a/build.ninja
            mylibrary/.externalNativeBuild/cmake/debug/armeabi-v7a/build.ninja
            mylibrary/.externalNativeBuild/cmake/debug/x86/build.ninja
            mylibrary/.externalNativeBuild/cmake/debug/x86_64/build.ninja
            mylibrary/.externalNativeBuild/cmake/release/armeabi-v7a/build.ninja
            mylibrary/.externalNativeBuild/cmake/release/x86/build.ninja
            mylibrary/.externalNativeBuild/cmake/release/x86_64/build.ninja
            mylibrary-2/.externalNativeBuild/cmake/debug/arm64-v8a/build.ninja
            mylibrary-2/.externalNativeBuild/cmake/debug/armeabi-v7a/build.ninja
            mylibrary-2/.externalNativeBuild/cmake/debug/x86/build.ninja
            mylibrary-2/.externalNativeBuild/cmake/debug/x86_64/build.ninja
            mylibrary-2/.externalNativeBuild/cmake/release/arm64-v8a/build.ninja
            mylibrary-2/.externalNativeBuild/cmake/release/armeabi-v7a/build.ninja
            mylibrary-2/.externalNativeBuild/cmake/release/x86/build.ninja
            mylibrary-2/.externalNativeBuild/cmake/release/x86_64/build.ninja
            mylibrary-3/.externalNativeBuild/cmake/debug/arm64-v8a/build.ninja
            mylibrary-3/.externalNativeBuild/cmake/debug/armeabi-v7a/build.ninja
            mylibrary-3/.externalNativeBuild/cmake/debug/x86/build.ninja
            mylibrary-3/.externalNativeBuild/cmake/debug/x86_64/build.ninja
            mylibrary-3/.externalNativeBuild/cmake/release/arm64-v8a/build.ninja
            mylibrary-3/.externalNativeBuild/cmake/release/armeabi-v7a/build.ninja
            mylibrary-3/.externalNativeBuild/cmake/release/x86/build.ninja
            mylibrary-3/.externalNativeBuild/cmake/release/x86_64/build.ninja
            mylibrary-4/.externalNativeBuild/cmake/debug/arm64-v8a/build.ninja
            mylibrary-4/.externalNativeBuild/cmake/debug/armeabi-v7a/build.ninja
            mylibrary-4/.externalNativeBuild/cmake/debug/x86/build.ninja
            mylibrary-4/.externalNativeBuild/cmake/debug/x86_64/build.ninja
            mylibrary-4/.externalNativeBuild/cmake/release/arm64-v8a/build.ninja
            mylibrary-4/.externalNativeBuild/cmake/release/armeabi-v7a/build.ninja
            mylibrary-4/.externalNativeBuild/cmake/release/x86/build.ninja
            mylibrary-4/.externalNativeBuild/cmake/release/x86_64/build.ninja
        """)

        val map = paths.removeCommonSegments()
        assertThat(map).hasSize(paths.size)
        assertThat(map.values).containsExactlyElementsIn(paths.map { it.toForwardSlashString()})
        println(map)
    }
}