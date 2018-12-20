package com.github.jomof.ninja

import org.junit.Test
import java.io.StringReader

class ZipNinjasKtTest {
    @Test
    fun basicZip() {
        val ninja = parseNinja("C:\\a\\b\\c\\d\\e\\f\\g", StringReader("""
        rule CXX_COMPILER__native-lib
          deps = gcc

        build CMakeFiles/native-lib.dir/myfile.cpp.o: CXX_COMPILER__native-lib ../../../../myfile.cpp || cmake_object_order_depends_target_native-lib
          DEFINES = -Dnative_lib_EXPORTS
        """.trimIndent()))

    }
}