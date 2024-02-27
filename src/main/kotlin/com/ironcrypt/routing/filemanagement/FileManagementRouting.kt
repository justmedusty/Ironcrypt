package com.ironcrypt.routing.filemanagement

import com.ironcrypt.database.*
import com.ironcrypt.enums.Maximums
import com.ironcrypt.enums.Maximums.MAX_FILE_NAME_CHAR_LENGTH
import encryptFile
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.errors.*

private fun validateFileName(originalFileName: String?): String =
    originalFileName?.takeUnless { it.length > MAX_FILE_NAME_CHAR_LENGTH.value } ?: ("file" + System.currentTimeMillis()
        .toString())


fun Application.configureFileManagementRouting() {
    routing {
        authenticate("jwt") {
            post("/ironcrypt/file/upload") {
                val multiPartData = call.receiveMultipart()
                val ownerId = this.call.principal<JWTPrincipal>()?.payload?.subject
                val userPublicKey = getPublicKey(getUserName(ownerId.toString()).toString())
                var fileName: String? = null
                var fileBytes: ByteArray? = null
                validateFileName(fileName)



                multiPartData.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            if (part.name == "file_name") {
                                fileName = part.value
                                validateFileName(fileName)
                            }
                        }

                        is PartData.FileItem -> {
                            fileBytes = part.streamProvider().readBytes()
                        }

                        else -> {
                            throw IOException("Error reading file")
                        }

                    }
                    part.dispose()
                }
                if (fileBytes != null && (fileBytes?.size!! > Maximums.MAX_FILE_SIZE_BYTES.value)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("Response" to "File exceeds maximum allowed size (1GB)")
                    )
                }
                if (fileName != null && fileBytes != null && ownerId != null && userPublicKey != null) {
                    val encryptedFile: ByteArray = encryptFile(userPublicKey, fileBytes!!)
                    uploadFile(
                        ownerId.toInt(), fileName!!, encryptedFile
                    )
                    call.respond(HttpStatusCode.OK, mapOf("Response" to "File Uploaded Successfully"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Missing file or file name"))
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
                            HttpStatusCode.BadRequest,
                            mapOf("Response" to "You are not authorized to delete this file")
                        )
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Invalid file ID or owner ID"))
                }
            }
            /*
            get("/ironcrypt/file/download/{fileId}"){
                val fileId = call.parameters["fileId"]?.toIntOrNull()
                val ownerId: Int? = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
                if (fileId != null && ownerId != null) {
                    val fileOwnerId: Int? = getOwnerId(fileId)
                    if (fileOwnerId == ownerId) {
                        val file: File? = getFile(fileId)
                        if (file != null) {
                            val decryptedFile: ByteArray = decryptFile(file.encryptedFile)
                            call.respondBytes(
                                decryptedFile,
                                ContentType.Application.OctetStream,
                                ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, file.fileName)
                            )
                        } else {
                            call.respond(HttpStatusCode.NotFound, mapOf("Response" to "File not found"))
                        }
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("Response" to "You are not authorized to download this file")
                        )
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Invalid file ID or owner ID"))
                }

            }
             */
        }


    }
}

