package com.ironcrypt.routing.filemanagement

import com.ironcrypt.enums.Maximums.MAX_FILE_NAME_CHAR_LENGTH
import com.ironcrypt.fileio.fileDeletion
import com.ironcrypt.fileio.fileDownload
import com.ironcrypt.fileio.fileUpload
import io.ktor.server.application.*
import io.ktor.server.auth.*
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
        }

    }
}


