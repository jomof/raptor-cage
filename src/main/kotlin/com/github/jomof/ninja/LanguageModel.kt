package com.github.jomof.ninja

import java.lang.RuntimeException

interface Node
interface Literal : Node
interface NinjaFile : Node

data class IdentifierRef(val value: String) : Node
data class UninstantiatedLiteral(val value: String) : Literal
data class InstantiatedLiteral(val value: String) : Literal
data class NinjaFileRef(val value: String) : NinjaFile
data class NinjaFileDef(
        val folder : String,
        val tops: List<Node>) : NinjaFile
data class NinjaFileNotFound(val value: String) : NinjaFile
data class BuildRef(val value: String, val original : BuildRef? = null) : Node
data class RuleRef(val value: String, val original : RuleRef? = null) : Node
data class PoolRef(val value: String, val original : PoolRef? = null) : Node

/**
 * Lexical include into the current folder. Relative to the folder that ninja.exe was invoked in.
 *
 * include rules.ninja
 */
data class Include(
        val file: NinjaFile
) : Node

data class SubNinja(
        val file: NinjaFile,
        val original: NinjaFileRef
) : Node

/**
 * ninja_required_version = 1.5
 */
data class Assignment(
        val name: IdentifierRef,
        val value: Literal
) : Node

/**
 * Sets the default targets if none are specified from the command-line
 *
 * default all
 */
data class Default(
        val file: List<BuildRef>
) : Node

/**
 * build CMakeFiles/edit_cache.util: CUSTOM_COMMAND
 *   COMMAND = cmd.exe /C "cd /D C:\Users\Jomo\AndroidStudioProjects\MyApplication\app\.externalNativeBuild\cmake\release\arm64-v8a && C:\Users\Jomo\AppData\Local\Android\Sdk\cmake\3.10.2.4988404\bin\cmake.exe -E echo "No interactive CMake dialog available.""
 *   DESC = No interactive CMake dialog available...
 *   restat = 1
 */
data class BuildDef(
        val explicitOutputs: List<BuildRef>,
        val implicitOutputs: List<BuildRef>,
        val rule: RuleRef,
        val explicitInputs: List<BuildRef>,
        val implicitInputs: List<BuildRef>,
        val orderOnlyInputs: List<BuildRef>,
        val properties: List<Assignment>
) : Node

/**
 * rule CXX_SHARED_LIBRARY_LINKER__native-lib
 *   command = cmd.exe /C "$PRE_LINK && C:\Users\Jomo\AppData\Local\Android\Sdk\ndk-bundle\toolchains\llvm\prebuilt\windows-x86_64\bin\clang++.exe --target=aarch64-none-linux-android21 --gcc-toolchain=C:/Users/Jomo/AppData/Local/Android/Sdk/ndk-bundle/toolchains/llvm/prebuilt/windows-x86_64 -fPIC $LANGUAGE_COMPILE_FLAGS $ARCH_FLAGS $LINK_FLAGS -shared $SONAME_FLAG$SONAME -o $TARGET_FILE $in $LINK_PATH $LINK_LIBRARIES && $POST_BUILD"
 *   description = Linking CXX shared library $TARGET_FILE
 *   restat = $RESTAT
 */
data class RuleDef(
        val name: RuleRef,
        val properties: List<Assignment>
) : Node


data class PoolDef(
        val name: PoolRef,
        val properties: List<Assignment>
) : Node