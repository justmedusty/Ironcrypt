package com.ironcrypt.routing.filemanagement

import com.ironcrypt.database.getPublicKey
import com.ironcrypt.database.getUserName
import com.ironcrypt.enums.Maximums
import com.ironcrypt.enums.Maximums.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

private fun validateFileName(originalFileName: String?): String =
    originalFileName?.takeUnless { it.length > Maximums.MAX_FILE_NAME_CHAR_LENGTH.value }
        ?: ("file" + System.currentTimeMillis().toString())

/*
fun Application.configureFileManagementRouting() {
    routing {
        authenticate("jwt") {
            post("/ironcrypt/file/upload") {
                val multiPartData = call.receiveMultipart()
                val ownerId = this.call.principal<JWTPrincipal>()?.payload?.subject
                val userPublicKey = getPublicKey(getUserName(ownerId.toString()).toString())
                var fileName: String? = null
                val fileBytes: ByteArray? = null
                validateFileName(fileName)



                multiPartData.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem {
                            val fileBytes = part.streamProvider().readBytes()
                            val FileName = part.originalFileName ?: "yourfile"
                            val filePath = ""
                        }
                    }
                }

            }
        }
    }
}
 */
