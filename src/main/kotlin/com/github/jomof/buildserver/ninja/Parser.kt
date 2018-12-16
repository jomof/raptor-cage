package com.github.jomof.buildserver.ninja

import java.io.StringReader

fun parseNinja(reader : StringReader) : NinjaFile {
    return NinjaFileDef(listOf())
}