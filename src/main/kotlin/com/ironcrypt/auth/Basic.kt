package com.ironcrypt.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureBasic() {
    authentication {
        basic {
            realm = "ironcrypt"
            validate { credentials ->
                if (credentials.name == credentials.password) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }


    }
}