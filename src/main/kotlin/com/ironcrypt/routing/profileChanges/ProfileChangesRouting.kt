import com.ironcrypt.database.*
import com.ironcrypt.fileio.deleteUserDir
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.exceptions.ExposedSQLException

/**
 * Configure profile change routes
 *
 */

private const val USERNAME_CHANGE_ERROR = "Username change failed with error"
private const val USERNAME_CHANGE_SUCCESS = "Username change success"
private const val USERNAME_REQUIREMENTS =
    "Please provide a valid username. Must be between 6 and 45 characters and be unique"
private const val PASSWORD_CHANGE_REQUIREMENTS = "Please provide a valid password. Must be at least 8 characters"
private const val PASSWORD_CHANGE_SUCCESS = "Password updated successfully"
private const val PASSWORD_CHANGE_ERROR = "Error updating password"
private const val ACCOUNT_DELETION_SUCCESS = "Account deleted successfully"
private const val ACCOUNT_DELETION_ERROR = "Error deleting your account"

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
                        mapOf("Response" to USERNAME_REQUIREMENTS),
                    )
                } else {
                    try {
                        updateUserCredentials(getUserName(id).toString(), false, newUserName)
                        call.respond(HttpStatusCode.OK, mapOf("Response" to USERNAME_CHANGE_SUCCESS))
                        logger.info { "user with id : $id changed userName to $newUserName" }
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to USERNAME_CHANGE_ERROR))
                    }
                }
            }
            post("/ironcrypt/profile/changePassword") {
                val postParams = call.receiveParameters()
                val newPassword = postParams["newPassword"]
                val principal = call.principal<JWTPrincipal>()
                val id = principal?.payload?.subject

                if (newPassword.isNullOrEmpty() || !userAndPasswordValidation("", newPassword)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("Response" to PASSWORD_CHANGE_REQUIREMENTS),
                    )
                } else {
                    try {
                        updateUserCredentials(
                            getUserName(id).toString(),
                            true,
                            newPassword,
                        )
                        call.respond(HttpStatusCode.OK, mapOf("Response" to PASSWORD_CHANGE_SUCCESS))
                    } catch (e: ExposedSQLException) {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to PASSWORD_CHANGE_ERROR))
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
                    deleteUserDir(userId)
                    call.respond(HttpStatusCode.OK, mapOf("Response" to ACCOUNT_DELETION_SUCCESS))
                    logger.info { "user with id : $id deleted account" }
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to ACCOUNT_DELETION_ERROR))
                }
            }
        }
    }
}