package com.ironcrypt.routing.filemanagement

import com.ironcrypt.database.*
import com.ironcrypt.encryption.encryptFileStream
import com.ironcrypt.enums.Maximums.MAX_FILE_NAME_CHAR_LENGTH
import com.ironcrypt.enums.Pathing
import com.ironcrypt.fileio.fileDownload
import fileDeletion
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.ByteArrayOutputStream
import java.io.OutputStream

private fun validateFileName(originalFileName: String?): String =
    originalFileName?.takeUnless { it.length > MAX_FILE_NAME_CHAR_LENGTH.value } ?: ("file" + System.currentTimeMillis()
        .toString())

private const val FAILED_CREATE_USER_DIR = "Failed to create user directory"
private const val FILE_TOO_LARGE = "File too large (Max 1GB)"
private const val INVALID_PARAM = "Invalid request parameters"
private const val INVALID_REQUEST = "Invalid Request"
private const val FILE_UPLOAD_SUCCESS = "File Upload Success!"


fun Application.configureFileManagementRouting() {
    routing {
        authenticate("jwt") {
            post("/ironcrypt/file/upload") {
                val userId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
                val directory = java.io.File(Pathing.USER_FILE_DIRECTORY.value + userId)
                val contentLength = call.request.contentLength()

                if (!directory.exists()) {
                    directory.mkdirs()
                }


                val multipart = call.receiveMultipart()
                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {


                        // retrieve file name of upload
                        val name = part.originalFileName!!
                        if (userId != null) {
                            if (contentLength != null) {
                                addFileData(userId,name,contentLength.toInt())
                            }
                        }
                        else {
                            call.respond(HttpStatusCode.Conflict)
                        }
                        val file = java.io.File(Pathing.USER_FILE_DIRECTORY.value + userId.toString() +"/$name" + ".gpg")

                        file.outputStream().use { outputStream ->
                            val encryptedOutputStream = ByteArrayOutputStream()
                            encryptFileStream(
                                getPublicKey(userId!!).toString(), part.streamProvider(), outputStream
                            )
                            encryptedOutputStream.writeTo(outputStream)
                            call.respond(HttpStatusCode.OK)


                        }

                    }
                    // make sure to dispose of the part after use to prevent leaks
                    part.dispose()
                }
            }




        delete("/ironcrypt/file/delete/{fileId}") {
            fileDeletion(this.call)
        }
        get("/ironcrypt/file/download/{fileId}") {
            fileDownload(this.call)
        }

        get("/ironcrypt/file/fetch") {
            val userId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 25
            if (userId != null) {
                val files: List<File>? = getAllFiles(userId)
                if (files?.isNotEmpty() == true) {
                    call.respond(
                        HttpStatusCode.OK, mapOf(
                            page to page, limit to limit, files to files
                        )
                    )
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("Response" to "No files found"))
                }

            }
            call.respond(
                HttpStatusCode.Conflict, mapOf("Response" to "An error occurred, could not determine your identity")
            )
        }
    }

}
}


