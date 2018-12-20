package com.github.jomof.ninja

import org.junit.Test
import java.io.StringReader

class ZipNinjasKtTest {
    @Test
    fun basicZip() {
        val ninja1 = parseNinja("C:\\a\\b\\c\\d\\e\\f\\x86", StringReader("""
        rule CXX_COMPILER__native-lib
          deps = gcc

        build CMakeFiles/native-lib.dir/myfile.cpp.o: CXX_COMPILER__native-lib ../../../../myfile.cpp || cmake_object_order_depends_target_native-lib
          DEFINES = -Dnative_lib_EXPORTS
        """.trimIndent()))
        val ninja2 = ninja1.copy(folder = "C:\\a\\b\\c\\d\\e\\f\\x86_64")
        val ninjaAbsolute1 = makePathsAbsolute(ninja1)
        val ninjaAbsolute2 = makePathsAbsolute(ninja2)

    }
}