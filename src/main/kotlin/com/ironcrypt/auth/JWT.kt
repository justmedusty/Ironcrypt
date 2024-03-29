package com.ironcrypt.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

// Please read the jwt property from the config file if you are using EngineMain

fun Application.configureJWT() {
    val jwtAudience = "ironcrypt-user"
    val jwtDomain = "https://jwt-provider-domain/"
    val jwtRealm = "ironcrypt"
    val jwtSecret = System.getenv("JWT_SECRET")
    authentication {
        jwt("jwt") {
            realm = jwtRealm
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret)).withAudience(jwtAudience).withIssuer(jwtDomain).build(),
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
