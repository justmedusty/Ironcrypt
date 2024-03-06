import com.ironcrypt.database.User
import com.ironcrypt.database.createUser
import com.ironcrypt.database.getUserId
import com.ironcrypt.database.userNameAlreadyExists
import com.ironcrypt.fileio.createUserDir
import com.ironcrypt.security.JWTConfig
import com.ironcrypt.security.createJWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Signup
 *
 * @property username
 * @property password
 * @constructor Create empty Signup
 */
data class Signup(val username: String, val password: String)

/**
 * Configure login
 *
 */
fun Application.configureLogin() {
    // Route for user login with Basic Auth

    routing {
        authenticate("basic") {
            post("/ironcrypt/login") {
                val principal = call.principal<UserIdPrincipal>() ?: error("Invalid credentials")
                val userName = principal.name
                val token = (createJWT(
                    JWTConfig(
                        "ironcrypt-user",
                        "https://jwt-provider-domain/",
                        System.getenv("JWT_SECRET"),
                        getUserId(userName),
                        900000,
                    ),
                ))
                call.respond(mapOf("access_token" to token))
            }

        }
        post("/ironcrypt/signup") {
            val signup = call.receive<Signup>()
            val user = User(signup.username, "null", signup.password, false)
            when {
                user.userName.length < 6 || user.userName.length > 45 -> {
                    call.respond(
                        HttpStatusCode.Conflict, mapOf("Response" to "Username must be between 6 and 45 characters")
                    )
                }

                user.passwordHash.length < 8 -> {
                    call.respond(HttpStatusCode.Conflict, mapOf("Response" to "Password must be at least 8 chars"))
                }

                userNameAlreadyExists(signup.username) -> {
                    call.respond(
                        HttpStatusCode.Conflict, mapOf("Response" to "This username is taken, please try another")
                    )
                }

                else -> {
                    createUser(user)
                    createUserDir(getUserId(user.userName))
                    call.respond(HttpStatusCode.OK, mapOf("Response" to "Successfully created your account"))
                }
            }
        }
    }
}