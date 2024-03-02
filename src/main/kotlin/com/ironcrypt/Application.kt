package com.ironcrypt

import com.ironcrypt.auth.configureBasic
import com.ironcrypt.configuration.configureDatabase
import com.ironcrypt.configuration.configureSerialization
import com.ironcrypt.plugins.*
import com.ironcrypt.routing.filemanagement.configureFileManagementRouting
import com.ironcrypt.auth.configureJWT
import configureKeyManagementRouting
import configureLogin
import configureProfileChangeRoutes
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 6969, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureBasic()
    configureHTTP()
    configureSerialization()
    configureDatabase()
    configureJWT()
    configureProfileChangeRoutes()
    configureKeyManagementRouting()
    configureFileManagementRouting()
    configureLogin()
}
