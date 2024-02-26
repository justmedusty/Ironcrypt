package com.ironcrypt.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureBasic() {
    authentication {
        basic {
            realm = "Ktor Server"
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