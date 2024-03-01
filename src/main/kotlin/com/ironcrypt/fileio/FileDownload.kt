package com.ironcrypt.fileio

import com.ironcrypt.database.getFileData
import com.ironcrypt.database.getOwnerId
import com.ironcrypt.enums.Pathing
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

private const val NOT_OWNER = "You are not the owner of this file"
private const val DOWNLOAD_FAILURE = "Download failed"
private const val FILE_NOT_FOUND = "File not found at specified path"
private const val INVALID_REQUEST = "Invalid request parameters"
suspend fun fileDownload(call: ApplicationCall) {
    val ownerId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
    val parameters = call.parameters
    val fileID = parameters["fileId"]?.toIntOrNull()
    if (fileID != null) {
        if (ownerId != null) {
            isFileOwner(call, ownerId, fileID)
        }
        val filePath: Path? = createFilePath(fileID).toPath()
        if (filePath != null) {
            createFilePath(fileID)
            handleDownload(call, filePath)
        }

    } else run {
        call.respond(HttpStatusCode.BadRequest, mapOf("Response" to INVALID_REQUEST))
    }

}

private fun createFilePath(fileID: Int): File {
    val fileMetaData: com.ironcrypt.database.File? = getFileData(fileID)
    val directory = File(Pathing.USER_FILE_DIRECTORY.value + "$fileMetaData.userID")
    if (!directory.exists()) {
        directory.mkdirs()
    }
    return directory
}

private suspend fun handleDownload(call: ApplicationCall, filePath: Path) {
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
    } else call.respond(HttpStatusCode.BadRequest, mapOf("Response" to FILE_NOT_FOUND))

}

private suspend fun isFileOwner(call: ApplicationCall, ownerId: Int, fileId: Int) {
    val fileOwner: Int? = getOwnerId(fileId)
    if (fileOwner != ownerId) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("Response" to NOT_OWNER))
    }
}