package com.github.jomof.ninja

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.StringReader

class NinjaParserKtTest {

    @Test
    fun empty() {
        parseNinja(StringReader(""))
    }

    @Test
    fun rules() {
        val ninja = parseNinja(StringReader("rule cat\n" +
                "  command = cat \$in > \$out\n" +
                "\n" +
                "rule date\n" +
                "  command = date > \$out\n" +
                "\n" +
                "build result: cat in_1.cc in-2.O\n"))
        println(ninja)
        assertThat(((ninja.tops[1] as RuleDef).properties[0].value as UninstantiatedLiteral).value)
                .isEqualTo("date > \$out")
    }

    @Test
    fun ruleAttributes() {
        parseNinja(StringReader("rule cat\n" +
                "  command = a\n" +
                "  depfile = a\n" +
                "  deps = a\n" +
                "  description = a\n" +
                "  generator = a\n" +
                "  restat = a\n" +
                "  rspfile = a\n" +
                "  rspfile_content = a\n"))
    }

    @Test
    fun indentedComments() {
        parseNinja(StringReader("rule cat\n" +
                "  command = a\n" +
                "  depfile = a\n" +
                "  # Deps comment\n" +
                "  deps = a\n" +
                "  description = a\n" +
                "  generator = a\n" +
                "  restat = a\n" +
                "  rspfile = a\n" +
                "  rspfile_content = a\n"))
    }

    @Test
    fun buildWithNoInputs() {
        val ninja = parseNinja(StringReader("build cat : Rule"))
        println(ninja)
    }

    @Test
    fun indentedCommentsAfterRule() {
        val ninja = parseNinja(StringReader("rule cat\n" +
                "  #command = a"))
        println(ninja)
    }

    @Test
    fun backslash() {
        val ninja = parseNinja(StringReader("foo = bar\\baz\n" +
                "foo2 = bar\\ baz\n"))
        println(ninja)
        val assign = ninja.tops[1] as Assignment
        val literal = assign.value as UninstantiatedLiteral
        assertThat(literal.value).isEqualTo("bar\\ baz")
    }

    @Test
    fun indentedCommentsAfterBuild() {
        val ninja = parseNinja(StringReader("build cat: Rule\n" +
                "  #command = a"))
        println(ninja)
    }

    @Test
    fun commentNoComment() {
        val ninja = parseNinja(StringReader("# this is a comment\n" +
                "foo = not # a comment\n"))
        val assignment = ninja.tops[0] as Assignment
        val literal = assignment.value as UninstantiatedLiteral
        assertThat(literal.value).isEqualTo("not # a comment")
    }

    @Test
    fun indentedBlankLine() {
        val ninja = parseNinja(StringReader("build cat: Rule\n" +
                "  \n" +
                "  command = a"))
        println(ninja)
    }

    @Test
    fun dollars() {
        val ninja = parseNinja(StringReader("rule foo\n" +
                "  command = \${out}bar\$\$baz\$\$\$\n" +
                "blah\n" +
                "x = \$\$dollar\n" +
                "build \$x: foo y\n"))
        val rule = ninja.tops[0] as RuleDef
        val assignment = rule.properties[0]
        val literal = assignment.value as UninstantiatedLiteral
        assertThat(literal.value).isEqualTo("\${out}bar\$baz\$blah")
        println(ninja)
    }

    @Test
    fun continuation() {
        val ninja = parseNinja(StringReader("rule link\n" +
                "  command = foo bar $\n" +
                "    baz\n" +
                "\n" +
                "build a: link c $\n" +
                " d e f\n"))
        println(ninja)
    }

    @Test
    fun ignoreTrailingComment() {
        parseNinja(StringReader("rule cat # My comment"))
    }

    @Test
    fun assignment() {
        val ninja = parseNinja(StringReader("a=b"))
        assertThat(ninja).isEqualTo(NinjaFileDef(listOf(
                Assignment(IdentifierRef("a"), UninstantiatedLiteral("b")))))
    }

    @Test
    fun twoAssign() {
        val ninja = parseNinja(StringReader("""
            a=b
            x=y
        """.trimIndent()))
        assertThat(ninja.tops[1]).isEqualTo(
                Assignment(IdentifierRef("x"), UninstantiatedLiteral("y")))
    }

    @Test
    fun include() {
        val ninja = parseNinja(StringReader("include xyz"))
        assertThat(ninja.tops[0]).isEqualTo(
                Include(NinjaFileRef("xyz")))
    }

    @Test
    fun default() {
        val ninja = parseNinja(StringReader("default abc xyz"))
        assertThat(ninja.tops[0].toString()).isEqualTo("Default(file=[BuildRef(value=abc), BuildRef(value=xyz)])")
    }

    @Test
    fun build() {
        val ninja = parseNinja(StringReader("build output.txt: RULE input.txt"))
        assertThat(ninja.tops[0].toString())
                .isEqualTo("BuildDef(outputs=[BuildRef(value=output.txt)], rule=RuleRef(value=RULE), inputs=[BuildRef(value=input.txt)], properties=[])")
    }

    @Test
    fun buildProp() {
        val ninja = parseNinja(StringReader("""
            build output.txt: RULE input.txt
              property = value""".trimIndent()))
        assertThat(ninja.tops[0].toString())
                .isEqualTo("BuildDef(outputs=[BuildRef(value=output.txt)], rule=RuleRef(value=RULE), inputs=[BuildRef(value=input.txt)], properties=[Assignment(name=IdentifierRef(value=property), value=UninstantiatedLiteral(value=value))])")
    }

    @Test
    fun buildTwoProperties() {
        val ninja = parseNinja(StringReader("""
            build output.txt: RULE input.txt
              property = value
              property2 = value2""".trimIndent()))
        assertThat(ninja.tops[0].toString())
                .isEqualTo("BuildDef(outputs=[BuildRef(value=output.txt)], rule=RuleRef(value=RULE), inputs=[BuildRef(value=input.txt)], properties=[Assignment(name=IdentifierRef(value=property), value=UninstantiatedLiteral(value=value)), Assignment(name=IdentifierRef(value=property2), value=UninstantiatedLiteral(value=value2))])")
    }

    @Test
    fun buildTwoInputs() {
        val ninja = parseNinja(StringReader("""
            build output.txt: RULE input1.txt input2.txt
              property = value""".trimIndent()))
        assertThat(ninja.tops[0].toString())
                .isEqualTo("BuildDef(outputs=[BuildRef(value=output.txt)], rule=RuleRef(value=RULE), inputs=[BuildRef(value=input1.txt), BuildRef(value=input2.txt)], properties=[Assignment(name=IdentifierRef(value=property), value=UninstantiatedLiteral(value=value))])")
    }

    @Test
    fun buildTwoOutputs() {
        val ninja = parseNinja(StringReader("build output1.txt output2.txt: RULE input1.txt"))
        assertThat(ninja.tops[0].toString())
                .isEqualTo("BuildDef(outputs=[BuildRef(value=output1.txt), BuildRef(value=output2.txt)], rule=RuleRef(value=RULE), inputs=[BuildRef(value=input1.txt)], properties=[])")
    }

    @Test
    fun propertyWithSpaces() {
        val ninja = parseNinja(StringReader("""
            build CMakeFiles/edit_cache.util: CUSTOM_COMMAND
              COMMAND = cmd.exe /C "cd /D C:\a\b\c && C:\x\y\z\cmake.exe -E echo "No interactive CMake dialog available.""
              DESC = No interactive CMake dialog available...
              restat = 1""".trimIndent()))
        assertThat(ninja.tops[0].toString())
                .isEqualTo("BuildDef(outputs=[BuildRef(value=CMakeFiles/edit_cache.util)], rule=RuleRef(value=CUSTOM_COMMAND), inputs=[], properties=[Assignment(name=IdentifierRef(value=COMMAND), value=UninstantiatedLiteral(value=cmd.exe /C \"cd /D C:\\a\\b\\c && C:\\x\\y\\z\\cmake.exe -E echo \"No interactive CMake dialog available.\"\")), Assignment(name=IdentifierRef(value=DESC), value=UninstantiatedLiteral(value=No interactive CMake dialog available...)), Assignment(name=IdentifierRef(value=restat), value=UninstantiatedLiteral(value=1))])")
    }

    @Test
    fun emptyRule() {
        val ninja = parseNinja(StringReader("rule my_rule"))
        assertThat(ninja.tops[0].toString())
                .isEqualTo("RuleDef(name=RuleRef(value=my_rule), properties=[])")
    }

    @Test
    fun sampleRulesNinja() {
        parseNinja(StringReader("""
        # CMAKE generated file: DO NOT EDIT!
        # Generated by "Ninja" Generator, CMake Version 3.10

        # This file contains all the rules used to get the outputs files
        # built from the input files.
        # It is included in the main 'build.ninja'.

        # =============================================================================
        # Project: Project
        # Configuration: Release
        # =============================================================================
        # =============================================================================

        #############################################
        # Rule for running custom commands.

        rule CUSTOM_COMMAND
          command = ${'$'}COMMAND
          description = ${'$'}DESC


        #############################################
        # Rule for compiling CXX files.

        rule CXX_COMPILER__native-lib
          depfile = ${'$'}DEP_FILE
          deps = gcc
          command = C:\Users\Jomo\AppData\Local\Android\Sdk\ndk-bundle\toolchains\llvm\prebuilt\windows-x86_64\bin\clang++.exe --target=i686-none-linux-android16 --gcc-toolchain=C:/Users/Jomo/AppData/Local/Android/Sdk/ndk-bundle/toolchains/llvm/prebuilt/windows-x86_64  ${'$'}DEFINES ${'$'}INCLUDES ${'$'}FLAGS -MD -MT ${'$'}out -MF ${'$'}DEP_FILE -o ${'$'}out -c ${'$'}in
          description = Building CXX object ${'$'}out


        #############################################
        # Rule for linking CXX shared library.

        rule CXX_SHARED_LIBRARY_LINKER__native-lib
          command = cmd.exe /C "${'$'}PRE_LINK && C:\Users\Jomo\AppData\Local\Android\Sdk\ndk-bundle\toolchains\llvm\prebuilt\windows-x86_64\bin\clang++.exe --target=i686-none-linux-android16 --gcc-toolchain=C:/Users/Jomo/AppData/Local/Android/Sdk/ndk-bundle/toolchains/llvm/prebuilt/windows-x86_64 -fPIC ${'$'}LANGUAGE_COMPILE_FLAGS ${'$'}ARCH_FLAGS ${'$'}LINK_FLAGS -shared ${'$'}SONAME_FLAG${'$'}SONAME -o ${'$'}TARGET_FILE ${'$'}in ${'$'}LINK_PATH ${'$'}LINK_LIBRARIES && ${'$'}POST_BUILD"
          description = Linking CXX shared library ${'$'}TARGET_FILE
          restat = ${'$'}RESTAT


        #############################################
        # Rule for re-running cmake.

        rule RERUN_CMAKE
          command = C:\Users\Jomo\AppData\Local\Android\Sdk\cmake\3.10.2.4988404\bin\cmake.exe -HC:\Users\Jomo\AndroidStudioProjects\MyApplication\app\.externalNativeBuild\cxx\release\x86 -BC:\Users\Jomo\AndroidStudioProjects\MyApplication\app\.externalNativeBuild\cmake\release\x86
          description = Re-running CMake...
          generator = 1


        #############################################
        # Rule for cleaning all built files.

        rule CLEAN
          command = C:\Users\Jomo\AppData\Local\Android\Sdk\cmake\3.10.2.4988404\bin\ninja.exe -t clean
          description = Cleaning all built files...


        #############################################
        # Rule for printing all primary targets available.

        rule HELP
          command = C:\Users\Jomo\AppData\Local\Android\Sdk\cmake\3.10.2.4988404\bin\ninja.exe -t targets
          description = All primary targets available:
        """.trimIndent()))
    }

    @Test
    fun sampleBuildNinja() {
        parseNinja(StringReader("""
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
            # Utility command for edit_cache

            build CMakeFiles/edit_cache.util: CUSTOM_COMMAND
              COMMAND = cmd.exe /C "cd /D C:\Users\Jomo\AndroidStudioProjects\MyApplication\app\.externalNativeBuild\cmake\release\arm64-v8a && C:\Users\Jomo\AppData\Local\Android\Sdk\cmake\3.10.2.4988404\bin\cmake.exe -E echo "No interactive CMake dialog available.""
              DESC = No interactive CMake dialog available...
              restat = 1
            build edit_cache: phony CMakeFiles/edit_cache.util

            #############################################
            # Utility command for rebuild_cache

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
            # Utility command for edit_cache

            build C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/.externalNativeBuild/cxx/release/arm64-v8a/CMakeFiles/edit_cache.util: CUSTOM_COMMAND
              COMMAND = cmd.exe /C "cd /D C:\Users\Jomo\AndroidStudioProjects\MyApplication\app\.externalNativeBuild\cxx\release\arm64-v8a && C:\Users\Jomo\AppData\Local\Android\Sdk\cmake\3.10.2.4988404\bin\cmake.exe -E echo "No interactive CMake dialog available.""
              DESC = No interactive CMake dialog available...
              restat = 1
            build C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/.externalNativeBuild/cxx/release/arm64-v8a/edit_cache: phony C${'$'}:/Users/Jomo/AndroidStudioProjects/MyApplication/app/.externalNativeBuild/cxx/release/arm64-v8a/CMakeFiles/edit_cache.util

            #############################################
            # Utility command for rebuild_cache

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
            # Re-run CMake if any of its inputs changed.

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

        """.trimIndent()))
    }
}