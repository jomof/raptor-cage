package com.github.jomof.buildserver.common.flags

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ClangFlagsTest {
    private val basicCcFlags = listOf(
            "C:/Users/jomof/AppData/Local/Android/Sdk/ndk-bundle/toolchains/llvm/prebuilt/windows-x86_64/bin/clang++.exe",
            "--target=armv7-none-linux-androideabi16",
            "--gcc-toolchain=C:/Users/jomof/AppData/Local/Android/Sdk/ndk-bundle/toolchains/arm-linux-androideabi-4.9/prebuilt/windows-x86_64",
            "--sysroot=C:/Users/jomof/AppData/Local/Android/Sdk/ndk-bundle/sysroot",
            "-Dnative_lib_EXPORTS",
            "-isystem",
            "C:/Users/jomof/AppData/Local/Android/Sdk/ndk-bundle/sources/cxx-stl/llvm-libc++/include",
            "-isystem",
            "C:/Users/jomof/AppData/Local/Android/Sdk/ndk-bundle/sources/android/support/include",
            "-isystem",
            "C:/Users/jomof/AppData/Local/Android/Sdk/ndk-bundle/sources/cxx-stl/llvm-libc++abi/include",
            "-isystem",
            "C:/Users/jomof/AppData/Local/Android/Sdk/ndk-bundle/sysroot/usr/include/arm-linux-androideabi",
            "-g",
            "-DANDROID",
            "-ffunction-sections",
            "-funwind-tables",
            "-fstack-protector-strong",
            "-no-canonical-prefixes",
            "-march=armv7-a",
            "-mfloat-abi=softfp",
            "-mfpu=vfpv3-d16",
            "-mthumb",
            "-Wa,--noexecstack",
            "-Wformat ",
            "-Werror=format-security",
            "-std=c++11",
            "-O0",
            "-fno-limit-debug-info",
            "-fPIC",
            "-MD",
            "-MT",
            "CMakeFiles/native-lib.dir/native-lib.cpp.o",
            "-MF",
            "CMakeFiles/native-lib.dir/native-lib.cpp.o.d",
            "-o",
            "CMakeFiles/native-lib.dir/native-lib.cpp.o",
            "-c",
            "C:/Users/jomof/AndroidStudioProjects/AndroidCCacheExample/app/src/main/cpp/native-lib.cpp"
            )

    @Test
    fun tryGetCompileFlags() {
        val flags = ClangFlags(basicCcFlags)
        println(flags)
    }

    @Test
    fun fileExtensions() {
        val flags = ClangFlags(basicCcFlags)
        assertThat(flags.fileExtensions)
                .isEqualTo(setOf("exe", "o", "d", "cpp"))
    }

    @Test
    fun sourceFiles() {
        val flags = ClangFlags(basicCcFlags)
        assertThat(flags.sourceFiles)
                .isEqualTo(listOf("C:/Users/jomof/AndroidStudioProjects/AndroidCCacheExample/app/src/main/cpp/native-lib.cpp"))
        assertThat(flags.lastSourceFile)
                .isEqualTo("C:/Users/jomof/AndroidStudioProjects/AndroidCCacheExample/app/src/main/cpp/native-lib.cpp")
    }

    @Test
    fun sourceFilesInPostProcessEquivalent() {
        val flags = ClangFlags(basicCcFlags).toPostprocessEquivalent()
        assertThat(flags.sourceFiles)
                .isEqualTo(listOf("CMakeFiles/native-lib.dir/native-lib.cpp.o.ii"))
        assertThat(flags.lastSourceFile)
                .isEqualTo("CMakeFiles/native-lib.dir/native-lib.cpp.o.ii")
    }

    @Test
    fun isCcCompile() {
        val flags = ClangFlags(basicCcFlags)
        assertThat(flags.isCcCompile).isTrue()
        assertThat(flags.isCCompile).isFalse()
    }

    @Test
    fun operation() {
        val flags = ClangFlags(basicCcFlags)
        assertThat(flags.operation).isEqualTo(ClangOperation.CC_TO_O)
    }

    @Test
    fun toPreprocessor() {
        val flags = ClangFlags(basicCcFlags).toPreprocessEquivalent()
        assertThat(flags.isPreprocessorRun).isTrue()
        assertThat(flags.operation).isEqualTo(ClangOperation.CC_TO_II)
        assertThat(flags.lastOutput).endsWith(".ii")
    }

    @Test
    fun danglingFlag() {
        val flags = ClangFlags(listOf("-o"))
        assertThat(flags.flags).isEqualTo(listOf(UnidentifiedClangFlag("-o")))
    }

    @Test
    fun oneArgSeparate() {
        val flags = ClangFlags(listOf("-o", "output.o"))
        assertThat(flags.flags).isEqualTo(listOf(OneArgFlag("-o", "output.o", listOf("-o", "output.o"))))
        assertThat(flags.lastOutput).isEqualTo("output.o")
    }

    @Test
    fun oneArgSeparateDouble() {
        val flags = ClangFlags(listOf("--o", "output.o"))
        assertThat(flags.flags).isEqualTo(listOf(OneArgFlag("--o", "output.o", listOf("--o", "output.o"))))
    }

    @Test
    fun oneArgCombinedDouble() {
        val flags = ClangFlags(listOf("--o=output.o"))
        assertThat(flags.flags).isEqualTo(listOf(OneArgFlag("--o", "output.o", listOf("--o=output.o"))))
    }

    @Test
    fun postProcessRemovesIsystem() {
        val flags = ClangFlags(listOf(
                "-isystem=bob", "tom.cpp", "-o=tom.o"))
                .toPostprocessEquivalent()
        assertThat(flags.flags.map { it.flag })
                .isEqualTo(listOf("tom.o.ii", "-o tom.o"))
    }
}