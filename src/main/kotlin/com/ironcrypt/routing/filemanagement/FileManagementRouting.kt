package com.ironcrypt.routing.filemanagement

import com.ironcrypt.database.deleteFile
import com.ironcrypt.database.getOwnerId
import com.ironcrypt.enums.Maximums.MAX_FILE_NAME_CHAR_LENGTH
import com.ironcrypt.fileio.fileDownload
import com.ironcrypt.fileio.fileUpload
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private fun validateFileName(originalFileName: String?): String =
    originalFileName?.takeUnless { it.length > MAX_FILE_NAME_CHAR_LENGTH.value } ?: ("file" + System.currentTimeMillis()
        .toString())


fun Application.configureFileManagementRouting() {
    routing {
        authenticate("jwt") {
            post("/ironcrypt/file/upload") {
                fileUpload(this.call)
            }
        }
        delete("/ironcrypt/file/delete/{fileId}") {
            val fileId = call.parameters["fileId"]?.toIntOrNull()
            val ownerId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
            if (fileId != null && ownerId != null) {
                val fileOwnerId: Int? = getOwnerId(fileId)
                if (fileOwnerId == ownerId) {
                    deleteFile(fileId)
                    call.respond(HttpStatusCode.OK, mapOf("Response" to "File Deleted Successfully"))
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest, mapOf("Response" to "You are not authorized to delete this file")
                    )
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Invalid file ID or owner ID"))
            }
        }
        get("/ironcrypt/file/download/{fileId}") {
            fileDownload(this.call)
        }
    }


}


