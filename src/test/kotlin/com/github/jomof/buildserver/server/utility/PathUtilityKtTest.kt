package com.github.jomof.buildserver.server.utility

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.nio.file.Paths

class PathUtilityKtTest {

    @Test
    fun oneLine() {
        val paths = """
            mylibrary\.externalNativeBuild\cmake\debug\arm64-v8a\build.ninja
        """.trimIndent().split("\n").map { Paths.get(it) }.toSet()

        val map = paths.removeCommonSegments()
        assertThat(map).hasSize(paths.size)
        assertThat(map.values).containsExactlyElementsIn(paths.map { it.toForwardSlashString()})
        assertThat(map[""])
                .isEqualTo("mylibrary/.externalNativeBuild/cmake/debug/arm64-v8a/build.ninja")
        println(map)
    }

    @Test
    fun twoLines() {
        val paths = """
            mylibrary\.externalNativeBuild\cmake\debug\arm64-v8a\build.ninja
            mylibrary\.externalNativeBuild\cmake\debug\armeabi-v7a\build.ninja
        """.trimIndent().split("\n").map { Paths.get(it) }.toSet()

        val map = paths.removeCommonSegments()
        assertThat(map).hasSize(paths.size)
        assertThat(map.values).containsExactlyElementsIn(paths.map { it.toForwardSlashString()})
        assertThat(map["arm64-v8a"])
                .isEqualTo("mylibrary/.externalNativeBuild/cmake/debug/arm64-v8a/build.ninja")
        println(map)
    }

    @Test
    fun releaseDebug() {
        val paths = """
            mylibrary\.externalNativeBuild\cmake\debug\arm64-v8a\build.ninja
            mylibrary\.externalNativeBuild\cmake\release\armeabi-v7a\build.ninja
        """.trimIndent().split("\n").map { Paths.get(it) }.toSet()

        val map = paths.removeCommonSegments()
        assertThat(map).hasSize(paths.size)
        assertThat(map.values).containsExactlyElementsIn(paths.map { it.toForwardSlashString()})
        println(map)
        assertThat(map["debug/arm64-v8a"])
                .isEqualTo("mylibrary/.externalNativeBuild/cmake/debug/arm64-v8a/build.ninja")
    }

    @Test
    fun bigTest() {
        val paths = """
            mylibrary\.externalNativeBuild\cmake\debug\arm64-v8a\build.ninja
            mylibrary\.externalNativeBuild\cmake\debug\armeabi-v7a\build.ninja
            mylibrary\.externalNativeBuild\cmake\debug\x86\build.ninja
            mylibrary\.externalNativeBuild\cmake\debug\x86_64\build.ninja
            mylibrary\.externalNativeBuild\cmake\release\armeabi-v7a\build.ninja
            mylibrary\.externalNativeBuild\cmake\release\x86\build.ninja
            mylibrary\.externalNativeBuild\cmake\release\x86_64\build.ninja
            mylibrary-2\.externalNativeBuild\cmake\debug\arm64-v8a\build.ninja
            mylibrary-2\.externalNativeBuild\cmake\debug\armeabi-v7a\build.ninja
            mylibrary-2\.externalNativeBuild\cmake\debug\x86\build.ninja
            mylibrary-2\.externalNativeBuild\cmake\debug\x86_64\build.ninja
            mylibrary-2\.externalNativeBuild\cmake\release\arm64-v8a\build.ninja
            mylibrary-2\.externalNativeBuild\cmake\release\armeabi-v7a\build.ninja
            mylibrary-2\.externalNativeBuild\cmake\release\x86\build.ninja
            mylibrary-2\.externalNativeBuild\cmake\release\x86_64\build.ninja
            mylibrary-3\.externalNativeBuild\cmake\debug\arm64-v8a\build.ninja
            mylibrary-3\.externalNativeBuild\cmake\debug\armeabi-v7a\build.ninja
            mylibrary-3\.externalNativeBuild\cmake\debug\x86\build.ninja
            mylibrary-3\.externalNativeBuild\cmake\debug\x86_64\build.ninja
            mylibrary-3\.externalNativeBuild\cmake\release\arm64-v8a\build.ninja
            mylibrary-3\.externalNativeBuild\cmake\release\armeabi-v7a\build.ninja
            mylibrary-3\.externalNativeBuild\cmake\release\x86\build.ninja
            mylibrary-3\.externalNativeBuild\cmake\release\x86_64\build.ninja
            mylibrary-4\.externalNativeBuild\cmake\debug\arm64-v8a\build.ninja
            mylibrary-4\.externalNativeBuild\cmake\debug\armeabi-v7a\build.ninja
            mylibrary-4\.externalNativeBuild\cmake\debug\x86\build.ninja
            mylibrary-4\.externalNativeBuild\cmake\debug\x86_64\build.ninja
            mylibrary-4\.externalNativeBuild\cmake\release\arm64-v8a\build.ninja
            mylibrary-4\.externalNativeBuild\cmake\release\armeabi-v7a\build.ninja
            mylibrary-4\.externalNativeBuild\cmake\release\x86\build.ninja
            mylibrary-4\.externalNativeBuild\cmake\release\x86_64\build.ninja
        """.trimIndent().split("\n").map { Paths.get(it) }.toSet()

        val map = paths.removeCommonSegments()
        assertThat(map).hasSize(paths.size)
        assertThat(map.values).containsExactlyElementsIn(paths.map { it.toForwardSlashString()})
        println(map)
    }
}