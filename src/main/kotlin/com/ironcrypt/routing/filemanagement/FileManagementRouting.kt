package com.ironcrypt.routing.filemanagement

import com.ironcrypt.database.*
import com.ironcrypt.encryption.encryptFileStream
import com.ironcrypt.enums.Maximums
import com.ironcrypt.enums.Maximums.MAX_FILE_NAME_CHAR_LENGTH
import com.ironcrypt.enums.Pathing
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path

private fun validateFileName(originalFileName: String?): String =
    originalFileName?.takeUnless { it.length > MAX_FILE_NAME_CHAR_LENGTH.value } ?: ("file" + System.currentTimeMillis()
        .toString())

private const val FAILED_CREATE_USER_DIR = "Failed to create user directory"
private const val FILE_TOO_LARGE = "File too large (Max 1GB)"
private const val INVALID_PARAM = "Invalid request parameters"
private const val INVALID_REQUEST = "Invalid Request"
private const val FILE_UPLOAD_SUCCESS = "File Upload Success!"
private const val DOWNLOAD_FAILURE = "Download failure"
private const val ERROR_ON_DELETION = "Error deleting file"
private const val DELETE_SUCCESS = "Success Deleting File"
private const val NOT_OWNER = "You are not the owner of this file"

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

                if (contentLength != null) {
                    if (contentLength.toInt() > Maximums.MAX_FILE_SIZE_BYTES.value) {
                        call.respond(HttpStatusCode.PayloadTooLarge, mapOf("Response" to FILE_TOO_LARGE))
                    }


                }
                val multipart = call.receiveMultipart()
                if (userId != null) {
                    multipart.forEachPart { part ->
                        if (part is PartData.FileItem) {

                            val name = part.originalFileName
                            if (name != null && contentLength != null) {


                                val file =
                                    java.io.File(Pathing.USER_FILE_DIRECTORY.value + userId.toString() + "/$name" + ".gpg")
                                file.outputStream().use { outputStream ->
                                    val encryptedOutputStream = ByteArrayOutputStream()
                                    encryptFileStream(
                                        getPublicKey(userId).toString(), part.streamProvider(), outputStream
                                    )
                                    encryptedOutputStream.writeTo(outputStream)
                                    addFileData(userId, name, contentLength.toInt(), this.call)
                                    call.respond(HttpStatusCode.OK, mapOf("Response" to FILE_UPLOAD_SUCCESS))


                                }

                            } else {
                                call.respond(HttpStatusCode.Conflict, mapOf("Response" to INVALID_REQUEST))
                            }

                        }
                        part.dispose()
                    }

                }
            }




            delete("/ironcrypt/file/delete/{fileId}") {
                val params = call.parameters
                val fileID: Int? = params["fileId"]?.toIntOrNull()
                val ownerId: Int? = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
                val fileData: File? = fileID?.let { it1 -> getFileData(it1) }
                if (fileData != null) {
                    val filePath = Pathing.USER_FILE_DIRECTORY.value + "/$ownerId/${fileData.fileName}.gpg"
                    if (ownerId != null) {

                        val fileOwner = getOwnerId(fileID)

                        if (fileOwner != ownerId) {
                            call.respond(HttpStatusCode.Unauthorized, mapOf("Response" to NOT_OWNER))
                        } else {
                            withContext(Dispatchers.IO) {
                                try {
                                    Files.delete(Path.of(filePath))
                                    deleteFile(fileID)
                                    call.respond(HttpStatusCode.OK, mapOf("Response" to DELETE_SUCCESS))
                                } catch (e: Exception) {
                                    logger.error { "Error deleting file, ${e.message}" }
                                    call.respond(
                                        HttpStatusCode.InternalServerError, mapOf("Response" to ERROR_ON_DELETION)
                                    )
                                }

                            }
                        }

                    } else {
                        call.respond(HttpStatusCode.BadRequest, mapOf("Response" to INVALID_REQUEST))
                    }
                } else {
                    call.respond(HttpStatusCode.NoContent, mapOf("Response" to "Error: File data null"))
                }


            }
            get("/ironcrypt/file/download/{fileId}") {
                val ownerId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
                val parameters = call.parameters
                val fileID = parameters["fileId"]?.toIntOrNull()

                if (fileID != null && ownerId != null) {
                    val fileMetaData: File? = getFileData(fileID)
                    val directory = java.io.File(Pathing.USER_FILE_DIRECTORY.value + "$ownerId")

                    if (directory.exists() && fileMetaData != null) {
                        val filePath = directory.resolve(fileMetaData.fileName).toPath()
                        if (Files.exists(filePath)) {
                            try {
                                withContext(Dispatchers.IO) {
                                    Files.newInputStream(filePath)
                                }.use { inputStream ->
                                    call.respondOutputStream(ContentType.Application.OctetStream, HttpStatusCode.OK) {
                                        inputStream.copyTo(this)
                                    }
                                }
                            } catch (e: Exception) {
                                call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to DOWNLOAD_FAILURE))
                            }
                        }
                    }
                }
                call.respond(HttpStatusCode.BadRequest, mapOf("Response" to INVALID_REQUEST))
            }
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



