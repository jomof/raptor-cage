package com.github.jomof.buildserver.client

import com.github.jomof.buildserver.BuildInfo.BUILD_TIME_MILLIS
import com.github.jomof.buildserver.BuildInfo.PROJECT_VERSION
import com.github.jomof.buildserver.common.ServerName

fun doVersion(serverName : ServerName) {
    val connection = getOrStartServer(serverName)
    println("raptor_cage_version=$PROJECT_VERSION")
    println("raptor_cage_client_build_time=$BUILD_TIME_MILLIS")
    println("raptor_cage_server_build_time=${connection.serverBuildTime()}")
    println("raptor_cage_server_schema_version=${connection.version()}")
    System.exit(0)
}