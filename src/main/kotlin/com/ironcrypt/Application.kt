package com.ironcrypt

import com.ironcrypt.auth.configureBasic
import com.ironcrypt.configuration.configureDatabases
import com.ironcrypt.configuration.configureSerialization
import com.ironcrypt.plugins.*
import com.ironcrypt.auth.configureJWT
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureBasic()
    configureHTTP()
    configureSerialization()
    configureDatabases()
    configureRouting()
    configureJWT()
}
