package com.github.jomof.ninja

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.StringReader

class NinjaTokenReaderKtTest {

    @Test
    fun dots() {
        val lines = mutableListOf<String>()
        StringReader("foo.dots \$bar.dots \${bar.dots}\n").forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly("foo.dots", "\$bar.dots", "\${bar.dots}", END_OF_LINE_TOKEN, END_OF_FILE_TOKEN)
    }
    @Test
    fun skipComments() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            Non-comment
        """.trimIndent()).forEachNonComment { lines += it }
        assertThat(lines).containsExactly("Non-comment")
    }

    @Test
    fun skipCommentsTrims() {
        val lines = mutableListOf<String>()
        StringReader("""
                 # Comment
            Non-comment 1
              Non-comment 2
        """.trimIndent()).forEachNonComment { lines += it }
        assertThat(lines).containsExactly("Non-comment 1", "  Non-comment 2")
    }

    @Test
    fun skipCommentsRemovesBlankLines() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            Non-comment 1

            Non-comment 2
        """.trimIndent()).forEachNonComment { lines += it }
        assertThat(lines).containsExactly("Non-comment 1", "Non-comment 2")
    }

    @Test
    fun continuedLineSimple() {
        val lines = mutableListOf<String>()
        StringReader("""
            Simple
        """.trimIndent()).forEachContinuedLine { lines += it }
        assertThat(lines).containsExactly("Simple")
    }

    @Test
    fun continuedLine() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            Non-$
            comment
        """.trimIndent()).forEachContinuedLine { lines += it }
        assertThat(lines).containsExactly("Non-comment")
    }

    /**
     * https://ninja-build.org/manual.html
     * "Whitespace at the beginning of a line after a line continuation is also stripped."
     */
    @Test
    fun whitespaceAfterContinuation() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            Non-$
              comment $
                indented
        """.trimIndent()).forEachContinuedLine { lines += it }
        assertThat(lines).containsExactly("Non-comment indented")
    }

    @Test
    fun continuedLineBeforeAndAfter() {
        val lines = mutableListOf<String>()
        StringReader("""
            Before
            # Comment
            Non-$
            comment
            After
        """.trimIndent()).forEachContinuedLine { lines += it }
        assertThat(lines).containsExactly("Before", "Non-comment", "After")
    }

    @Test
    fun tokenizeSimpleBuild() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            output.txt : CREATE input.txt
        """.trimIndent()).forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly("output.txt", ":", "CREATE", "input.txt", END_OF_LINE_TOKEN, END_OF_FILE_TOKEN)
    }

    @Test
    fun tokenizeSimpleBuildNoSpaceAfterColon() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            output.txt :CREATE input.txt
        """.trimIndent()).forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly("output.txt", ":", "CREATE", "input.txt", END_OF_LINE_TOKEN, END_OF_FILE_TOKEN)
    }

    @Test
    fun tokenizeSimpleBuildNoSpaceBeforeColon() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            output.txt: CREATE input.txt
        """.trimIndent()).forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly("output.txt", ":", "CREATE", "input.txt", END_OF_LINE_TOKEN, END_OF_FILE_TOKEN)
    }

    @Test
    fun tokenizeSimpleBuildNoSpaceAroundColon() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            output.txt:CREATE input.txt
        """.trimIndent()).forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly("output.txt", ":", "CREATE", "input.txt", END_OF_LINE_TOKEN, END_OF_FILE_TOKEN)
    }

    @Test
    fun tokenizeBuildWithPipe() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            output.txt:CREATE | input1.txt input2.txt
        """.trimIndent()).forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly("output.txt", ":", "CREATE", "|", "input1.txt", "input2.txt", END_OF_LINE_TOKEN, END_OF_FILE_TOKEN)
    }

    @Test
    fun tokenizeIndentBlock() {
        val lines = mutableListOf<String>()
        StringReader("""
            # Comment
            output.txt:CREATE input.txt
              PROPERTY = value
        """.trimIndent()).forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly(
                "output.txt", ":", "CREATE", "input.txt", END_OF_LINE_TOKEN,
                INDENT_TOKEN, "PROPERTY", "=", "value", END_OF_LINE_TOKEN, END_OF_FILE_TOKEN)
    }

    @Test
    fun propertyWithNoSpaces() {
        val lines = mutableListOf<String>()
        StringReader("""
          PROPERTY=value
        """.trimIndent()).forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly("PROPERTY", "=", "value", END_OF_LINE_TOKEN, END_OF_FILE_TOKEN)
    }

    @Test
    fun propertyWithSpaces() {
        val lines = mutableListOf<String>()
        StringReader("""
          PROPERTY = value
        """.trimIndent()).forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly("PROPERTY", "=", "value", END_OF_LINE_TOKEN, END_OF_FILE_TOKEN)
    }

    @Test
    fun escapedColon() {
        val lines = mutableListOf<String>()
        StringReader("""
          PROPERTY=C$:\My$ File
        """.trimIndent()).forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly("PROPERTY", "=", "C:\\My File", END_OF_LINE_TOKEN, END_OF_FILE_TOKEN)
    }

    @Test
    fun trailingComment() {
        val lines = mutableListOf<String>()
        StringReader("""
          rule cat # My comment"
        """.trimIndent()).forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly("rule", "cat", END_OF_LINE_TOKEN, END_OF_FILE_TOKEN)
    }

    @Test
    fun colonInBuildInput() {
        val lines = mutableListOf<String>()
        StringReader("build build.ninja: RERUN_CMAKE C\$:/abc").forEachNinjaToken { lines += it }
        assertThat(lines).containsExactly("build", "build.ninja", ":", "RERUN_CMAKE", "C:/abc", END_OF_LINE_TOKEN, END_OF_FILE_TOKEN)
    }

    @Test
    fun bigTest() {
        val lines = mutableListOf<String>()
        StringReader("""
# CMAKE generated file: DO NOT EDIT!
# Generated by "Ninja" Generator, CMake Version 3.10

# This file contains all the build statements describing the
# compilation DAG.

# =============================================================================
# Write statements declared in CMakeLists.txt:
#
# Which is the root file.
# =============================================================================

# =============================================================================
# Project: Project
# Configuration: Release
# =============================================================================

#############################################
# Minimal version of Ninja required by this file

ninja_required_version = 1.5

# =============================================================================
# Include auxiliary files.


#############################################
# Include rules file.

include rules.ninja


#############################################
# utility command for edit_cache

build CMakeFiles/edit_cache.util: CUSTOM_COMMAND
  COMMAND = cmd.exe /C "cd /D C:\Users\Jomo\AndroidStudioProjects\MyApplication\app\.externalNativeBuild\cmake\release\arm64-v8a && C:\Users\Jomo\AppData\Local\Android\Sdk\cmake\3.10.2.4988404\bin\cmake.exe -E echo "No interactive CMake dialog available.""
  DESC = No interactive CMake dialog available...
  restat = 1
build edit_cache: phony CMakeFiles/edit_cache.util

#############################################
# utility command for rebuild_cache

build CMakeFiles/rebuild_cache.util: CUSTOM_COMMAND
  COMMAND = cmd.exe /C "cd /D C:\Users\Jomo\AndroidStudioProjects\MyApplication\app\.externalNativeBuild\cmake\release\arm64-v8a && C:\Users\Jomo\AppData\Local\Android\Sdk\cmake\3.10.2.4988404\bin\cmake.exe -HC:\Users\Jomo\AndroidStudioProjects\MyApplication\app\.externalNativeBuild\cxx\release\arm64-v8a -BC:\Users\Jomo\AndroidStudioProjects\MyApplication\app\.externalNativeBuild\cmake\release\arm64-v8a"
  DESC = Running CMake to regenerate build system...
  pool = console
  restat = 1
build rebuild_cache: phony CMakeFiles/rebuild_cache.util
# =============================================================================
# Write statements declared in CMakeLists.txt:
# C:/Users/Jomo/AndroidStudioProjects/MyApplication/app/.externalNativeBuild/cxx/release/arm64-v8a/CMakeLists.txt
# =============================================================================

# =============================================================================
# Object build statements for SHARED_LIBRARY target native-lib


#############################################
# Order-only phony target for native-lib

build cmake_object_order_depends_target_native-lib: phony
build C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/.externalNativeBuild/cxx/release/arm64-v8a/CMakeFiles/native-lib.dir/native-lib.cpp.o: CXX_COMPILER__native-lib C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/src/main/cpp/native-lib.cpp || cmake_object_order_depends_target_native-lib
  DEFINES = -Dnative_lib_EXPORTS
  DEP_FILE = C:\Users\Jomo\AndroidStudioProjects\MyApplication\app\.externalNativeBuild\cxx\release\arm64-v8a\CMakeFiles\native-lib.dir\native-lib.cpp.o.d
  FLAGS = --sysroot C:/Users/Jomo/AppData/Local/Android/Sdk/ndk-bundle/toolchains/llvm/prebuilt/windows-x86_64/sysroot -g -DANDROID -fdata-sections -ffunction-sections -funwind-tables -fstack-protector-strong -no-canonical-prefixes -Wa,--noexecstack -Wformat -Werror=format-security -stdlib=libc++ -std=c++11  -O2 -DNDEBUG  -fPIC
  OBJECT_DIR = C:\Users\Jomo\AndroidStudioProjects\MyApplication\app\.externalNativeBuild\cxx\release\arm64-v8a\CMakeFiles\native-lib.dir
  OBJECT_FILE_DIR = C:\Users\Jomo\AndroidStudioProjects\MyApplication\app\.externalNativeBuild\cxx\release\arm64-v8a\CMakeFiles\native-lib.dir

# =============================================================================
# Link build statements for SHARED_LIBRARY target native-lib


#############################################
# Link the shared library C:\Users\Jomo\AndroidStudioProjects\MyApplication\app\build\intermediates\cmake\release\obj\arm64-v8a\libnative-lib.so

build C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/build/intermediates/cmake/release/obj/arm64-v8a/libnative-lib.so: CXX_SHARED_LIBRARY_LINKER__native-lib C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/.externalNativeBuild/cxx/release/arm64-v8a/CMakeFiles/native-lib.dir/native-lib.cpp.o | C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/ndk-bundle/toolchains/llvm/prebuilt/windows-x86_64/sysroot/usr/lib/aarch64-linux-android/21/liblog.so
  LANGUAGE_COMPILE_FLAGS = --sysroot C:/Users/Jomo/AppData/Local/Android/Sdk/ndk-bundle/toolchains/llvm/prebuilt/windows-x86_64/sysroot -g -DANDROID -fdata-sections -ffunction-sections -funwind-tables -fstack-protector-strong -no-canonical-prefixes -Wa,--noexecstack -Wformat -Werror=format-security -stdlib=libc++ -std=c++11  -O2 -DNDEBUG
  LINK_FLAGS = -Wl,--exclude-libs,libgcc.a -Wl,--exclude-libs,libatomic.a -static-libstdc++ -Wl,--build-id -Wl,--warn-shared-textrel -Wl,--fatal-warnings -Wl,--no-undefined -Qunused-arguments -Wl,-z,noexecstack -Wl,-z,relro -Wl,-z,now
  LINK_LIBRARIES = -llog -latomic -lm
  LINK_PATH = -LC:/Users/Jomo/AppData/Local/Android/Sdk/ndk-bundle/toolchains/llvm/prebuilt/windows-x86_64/sysroot/usr/lib/aarch64-linux-android/21
  OBJECT_DIR = C:\Users\Jomo\AndroidStudioProjects\MyApplication\app\.externalNativeBuild\cxx\release\arm64-v8a\CMakeFiles\native-lib.dir
  POST_BUILD = cd .
  PRE_LINK = cd .
  SONAME = libnative-lib.so
  SONAME_FLAG = -Wl,-soname,
  TARGET_FILE = C:\Users\Jomo\AndroidStudioProjects\MyApplication\app\build\intermediates\cmake\release\obj\arm64-v8a\libnative-lib.so
  TARGET_PDB = native-lib.so.dbg

#############################################
# utility command for edit_cache

build C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/.externalNativeBuild/cxx/release/arm64-v8a/CMakeFiles/edit_cache.util: CUSTOM_COMMAND
  COMMAND = cmd.exe /C "cd /D C:\Users\Jomo\AndroidStudioProjects\MyApplication\app\.externalNativeBuild\cxx\release\arm64-v8a && C:\Users\Jomo\AppData\Local\Android\Sdk\cmake\3.10.2.4988404\bin\cmake.exe -E echo "No interactive CMake dialog available.""
  DESC = No interactive CMake dialog available...
  restat = 1
build C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/.externalNativeBuild/cxx/release/arm64-v8a/edit_cache: phony C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/.externalNativeBuild/cxx/release/arm64-v8a/CMakeFiles/edit_cache.util

#############################################
# utility command for rebuild_cache

build C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/.externalNativeBuild/cxx/release/arm64-v8a/CMakeFiles/rebuild_cache.util: CUSTOM_COMMAND
  COMMAND = cmd.exe /C "cd /D C:\Users\Jomo\AndroidStudioProjects\MyApplication\app\.externalNativeBuild\cxx\release\arm64-v8a && C:\Users\Jomo\AppData\Local\Android\Sdk\cmake\3.10.2.4988404\bin\cmake.exe -HC:\Users\Jomo\AndroidStudioProjects\MyApplication\app\.externalNativeBuild\cxx\release\arm64-v8a -BC:\Users\Jomo\AndroidStudioProjects\MyApplication\app\.externalNativeBuild\cmake\release\arm64-v8a"
  DESC = Running CMake to regenerate build system...
  pool = console
  restat = 1
build C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/.externalNativeBuild/cxx/release/arm64-v8a/rebuild_cache: phony C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/.externalNativeBuild/cxx/release/arm64-v8a/CMakeFiles/rebuild_cache.util
# =============================================================================
# Target aliases.

build libnative-lib.so: phony C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/build/intermediates/cmake/release/obj/arm64-v8a/libnative-lib.so
build native-lib: phony C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/build/intermediates/cmake/release/obj/arm64-v8a/libnative-lib.so
# =============================================================================
# Folder targets.

# =============================================================================
# =============================================================================
# =============================================================================
# Built-in targets


#############################################
# The main all target.

build all: phony C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/build/intermediates/cmake/release/obj/arm64-v8a/libnative-lib.so

#############################################
# Make the all target the default.

default all

#############################################
# Re-run CMake if any of its explicitInputs changed.

build build.ninja: RERUN_CMAKE | C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/.externalNativeBuild/cxx/release/arm64-v8a/CMakeLists.txt C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/src/main/cpp/CMakeLists.txt C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/src/main/cpp/muh_chain.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeCCompiler.cmake.in C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeCCompilerABI.c C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeCInformation.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeCXXCompiler.cmake.in C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeCXXCompilerABI.cpp C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeCXXInformation.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeCommonLanguageInclude.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeDetermineCCompiler.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeDetermineCXXCompiler.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeDetermineCompileFeatures.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeDetermineCompiler.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeDetermineCompilerABI.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeDetermineSystem.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeFindBinUtils.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeGenericSystem.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeLanguageInformation.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeParseImplicitLinkInfo.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeSystem.cmake.in C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeSystemSpecificInformation.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeSystemSpecificInitialize.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeTestCCompiler.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeTestCXXCompiler.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeTestCompilerCommon.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Compiler/CMakeCommonCompilerMacros.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Compiler/Clang-C-FeatureTests.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Compiler/Clang-C.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Compiler/Clang-CXX-FeatureTests.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Compiler/Clang-CXX-TestableFeatures.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Compiler/Clang-CXX.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Compiler/Clang-FindBinUtils.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Compiler/Clang.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Compiler/GNU.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Internal/FeatureTesting.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Android-Clang-C.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Android-Clang-CXX.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Android-Clang.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Android-Determine-C.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Android-Determine-CXX.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Android-Determine.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Android-Initialize.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Android.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Android/Determine-Compiler.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Linux.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/UnixPaths.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/ndk-bundle/build/cmake/android.toolchain.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/ndk-bundle/build/cmake/platforms.cmake CMakeCache.txt CMakeFiles/3.10.2/CMakeCCompiler.cmake CMakeFiles/3.10.2/CMakeCXXCompiler.cmake CMakeFiles/3.10.2/CMakeSystem.cmake CMakeFiles/feature_tests.c CMakeFiles/feature_tests.cxx
  pool = console

#############################################
# A missing CMake input file is not an error.

build C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/.externalNativeBuild/cxx/release/arm64-v8a/CMakeLists.txt C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/src/main/cpp/CMakeLists.txt C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/src/main/cpp/muh_chain.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeCCompiler.cmake.in C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeCCompilerABI.c C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeCInformation.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeCXXCompiler.cmake.in C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeCXXCompilerABI.cpp C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeCXXInformation.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeCommonLanguageInclude.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeDetermineCCompiler.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeDetermineCXXCompiler.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeDetermineCompileFeatures.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeDetermineCompiler.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeDetermineCompilerABI.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeDetermineSystem.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeFindBinUtils.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeGenericSystem.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeLanguageInformation.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeParseImplicitLinkInfo.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeSystem.cmake.in C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeSystemSpecificInformation.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeSystemSpecificInitialize.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeTestCCompiler.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeTestCXXCompiler.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/CMakeTestCompilerCommon.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Compiler/CMakeCommonCompilerMacros.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Compiler/Clang-C-FeatureTests.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Compiler/Clang-C.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Compiler/Clang-CXX-FeatureTests.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Compiler/Clang-CXX-TestableFeatures.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Compiler/Clang-CXX.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Compiler/Clang-FindBinUtils.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Compiler/Clang.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Compiler/GNU.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Internal/FeatureTesting.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Android-Clang-C.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Android-Clang-CXX.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Android-Clang.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Android-Determine-C.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Android-Determine-CXX.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Android-Determine.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Android-Initialize.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Android.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Android/Determine-Compiler.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/Linux.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/cmake/3.10.2.4988404/share/cmake-3.10/Modules/Platform/UnixPaths.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/ndk-bundle/build/cmake/android.toolchain.cmake C${'$'}:/Users/Jomo/AppData/Local/Android/Sdk/ndk-bundle/build/cmake/platforms.cmake CMakeCache.txt CMakeFiles/3.10.2/CMakeCCompiler.cmake CMakeFiles/3.10.2/CMakeCXXCompiler.cmake CMakeFiles/3.10.2/CMakeSystem.cmake CMakeFiles/feature_tests.c CMakeFiles/feature_tests.cxx: phony

#############################################
# Clean all the built files.

build clean: CLEAN

#############################################
# Print all primary targets available.

build help: HELP

        """.trimIndent()).forEachNinjaToken { lines += it }
    }
}