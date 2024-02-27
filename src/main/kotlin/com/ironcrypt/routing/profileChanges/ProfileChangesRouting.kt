import com.ironcrypt.database.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Configure profile change routes
 *
 */
fun Application.configureProfileChangeRoutes() {
    routing {
        authenticate("jwt") {
            post("/ironcrypt/profile/changeUserName") {
                val postParams = call.receiveParameters()
                val newUserName = postParams["newUser"] ?: error("No new value provided")
                val principal = call.principal<JWTPrincipal>()
                val id = principal?.payload?.subject

                if (newUserName.isEmpty() || !userAndPasswordValidation(newUserName, "")) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("Response" to "Please provide a valid username. Must be between 6 and 45 characters and be unique"),
                    )
                } else {
                    try {
                        updateUserCredentials(getUserName(id).toString(), false, newUserName)
                        call.respond(HttpStatusCode.OK, mapOf("Response" to "Username updated successfully"))
                        logger.info { "user with id : $id changed userName to $newUserName" }
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("Response" to e.message))
                    }
                }
            }
            post("/ironcrypt/profile/changePassword") {
                val postParams = call.receiveParameters()
                val newPassword = postParams["newPassword"] ?: error("No new value provided")
                val principal = call.principal<JWTPrincipal>()
                val id = principal?.payload?.subject

                if (newPassword.isEmpty() || !userAndPasswordValidation("", newPassword)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("Response" to "Please provide a valid password. Must be at least 8 characters"),
                    )
                } else {
                    try {
                        updateUserCredentials(
                            getUserName(id).toString(),
                            true,
                            newPassword,
                        )
                        call.respond(HttpStatusCode.OK, mapOf("Response" to "Password updated successfully"))
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("Response" to e.message))
                        logger.info { "user with id : $id changed their password" }
                    }
                }
            }
            delete("/ironcrypt/profile/deleteAccount") {
                val principal = call.principal<JWTPrincipal>()
                val id = principal?.payload?.subject

                val userId = id?.toIntOrNull()
                if (userId != null) {
                    deleteUser(userId)
                    call.respond(HttpStatusCode.OK, mapOf("Response" to "Account Deleted"))
                    logger.info { "user with id : $id deleted account" }
                } else {
                    call.respond(HttpStatusCode.Conflict, mapOf("Response" to "No Id Found"))
                }
            }
        }
    }
}