import com.ironcrypt.database.getOwnerId
import com.ironcrypt.database.logger
import com.ironcrypt.enums.Pathing
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path


private const val ERROR_ON_DELETION = "Error deleting file"
private const val DELETE_SUCCESS = "Success Deleting File"
private const val NOT_OWNER = "You are not the owner of this file"
suspend fun fileDeletion(call: ApplicationCall) {
    val params = call.parameters
    val fileID: Int? = params["fileId"]?.toIntOrNull()
    val ownerId: Int? = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
    val filePath = Pathing.USER_FILE_DIRECTORY.value + "/$ownerId/$fileID"

    if (ownerId != null && fileID != null) {

        val fileOwner = getOwnerId(fileID)

        if (fileOwner != ownerId) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("Response" to NOT_OWNER))
            return
        } else {
            withContext(Dispatchers.IO) {
                try {
                    Files.delete(Path.of(filePath))
                    call.respond(HttpStatusCode.OK, mapOf("Response" to DELETE_SUCCESS))
                } catch (e: Exception) {
                    logger.error { "Error deleting file, ${e.message}" }
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to ERROR_ON_DELETION))
                }

            }
        }

    }
}