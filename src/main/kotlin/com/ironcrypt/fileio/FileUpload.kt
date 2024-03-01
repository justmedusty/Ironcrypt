package com.ironcrypt.fileio

import com.ironcrypt.database.addFileData
import com.ironcrypt.database.getPublicKey
import com.ironcrypt.database.logger
import com.ironcrypt.encryption.encryptFileStream
import com.ironcrypt.enums.Maximums
import com.ironcrypt.enums.Pathing
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.inputStream

private const val FAILED_CREATE_USER_DIR = "Failed to create user directory"
private const val FILE_TOO_LARGE = "File too large (Max 1GB)"
private const val INVALID_PARAM = "Invalid request parameters"
private const val INVALID_REQUEST = "Invalid Request"
private const val FILE_UPLOAD_SUCCESS = "File Upload Success!"


suspend fun fileUpload(call: ApplicationCall) {
    val ownerId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
    val parameters = call.receiveMultipart().readAllParts().associateBy({ it.name ?: "" }, { it })
    val filePart = parameters["file"]
    val fileName = parameters["fileName"]?.let { part -> (part as? PartData.FormItem)?.value }

    if (filePart != null && fileName != null && ownerId != null) {
        handleFileUpload(call, ownerId, fileName)
    } else {
        call.respond(HttpStatusCode.BadRequest, mapOf("Response" to INVALID_REQUEST))
    }
}

private suspend fun handleFileUpload(call: ApplicationCall, ownerId: Int, fileName: String) {
    val directory = File(Pathing.USER_FILE_DIRECTORY.value + "$ownerId")
    val fileCreationSuccessful = directory.exists() || directory.mkdir()
    if (!fileCreationSuccessful) {
        call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to FAILED_CREATE_USER_DIR))
        return
    }

    val filePath = Paths.get(directory.absolutePath, fileName)
    val publicKey = getPublicKey(ownerId)
    val contentLength = call.request.contentLength()
    if (contentLength != null && contentLength > Maximums.MAX_FILE_SIZE_BYTES.value.toLong()) {
        call.respond(HttpStatusCode.BadRequest, FILE_TOO_LARGE)
        return
    }

    if (publicKey != null) {
        if (contentLength != null) {
            saveFileToDirectory(call, ownerId, filePath, publicKey, fileName, contentLength)
        }
    } else {
        call.respond(HttpStatusCode.BadRequest, mapOf("Response" to INVALID_PARAM))
    }
}

private suspend fun saveFileToDirectory(
    call: ApplicationCall, ownerId: Int, filePath: Path, publicKey: String, fileName: String, contentLength: Long
) {
    try {
        val encryptedOutputStream = ByteArrayOutputStream()
        encryptFileStream(publicKey, filePath.inputStream(), encryptedOutputStream)
        writeEncryptedFile(filePath, encryptedOutputStream)
        addFileData(ownerId, fileName, contentLength.toInt())
    } catch (e: Exception) {
        logger.error { "Error on file upload, deleting what was downloaded{$e.message}" }
        val file = File(filePath.toString())
        if (file.exists()) {
            file.delete()
        }
    }
    call.respond(HttpStatusCode.OK, mapOf("Response" to FILE_UPLOAD_SUCCESS))
}

private suspend fun writeEncryptedFile(filePath: Path, encryptedOutputStream: ByteArrayOutputStream) {
    withContext(Dispatchers.IO) {
        Files.write(filePath, encryptedOutputStream.toByteArray())
    }
}
