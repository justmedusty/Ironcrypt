package com.ironcrypt.routing.filemanagement

import com.ironcrypt.database.File
import com.ironcrypt.database.getAllFiles
import com.ironcrypt.enums.Maximums.MAX_FILE_NAME_CHAR_LENGTH
import com.ironcrypt.fileio.fileDownload
import com.ironcrypt.fileio.fileUpload
import fileDeletion
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
                    HttpStatusCode.Conflict,
                    mapOf("Response" to "An error occurred, could not determine your identity")
                )
            }
        }

    }
}


