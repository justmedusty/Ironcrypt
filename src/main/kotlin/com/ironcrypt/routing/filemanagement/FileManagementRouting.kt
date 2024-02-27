package com.ironcrypt.routing.filemanagement

import com.ironcrypt.database.getPublicKey
import com.ironcrypt.database.getUserName
import com.ironcrypt.database.uploadFile
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
                if (fileName != null && fileBytes != null && ownerId != null && userPublicKey != null) {
                    val encryptedFile: ByteArray = encryptFile(userPublicKey, fileBytes!!)
                    uploadFile(
                        ownerId.toInt(), fileName!!, encryptedFile
                    )
                    call.respond(HttpStatusCode.OK, mapOf("response" to "File Uploaded Successfully"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Missing file or file name")
                }

            }
        }
    }
}

