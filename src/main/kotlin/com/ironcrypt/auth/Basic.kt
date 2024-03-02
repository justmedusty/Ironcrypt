package com.ironcrypt.auth

import com.ironcrypt.database.verifyCredentials
import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureBasic() {
    authentication {
        basic(name="basic") {
            realm = "ironcrypt"
            validate { credentials ->
                if (verifyCredentials(credentials.name,credentials.password)) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }


    }
}