package com.ironcrypt.fileio

import com.ironcrypt.database.getFileData
import com.ironcrypt.database.getOwnerId
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
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.inputStream


suspend fun fileUpload(call: ApplicationCall) {
    val ownerId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
    val parameters = call.receiveMultipart().readAllParts().associateBy({ it.name ?: "" }, { it })
    val contentLength = call.request.contentLength()

    val filePart = parameters["file"]
    val fileName = parameters["fileName"]?.let { part -> (part as? PartData.FormItem)?.value }


    if (filePart != null && fileName != null && ownerId != null) {

        val directory = File(Pathing.USER_FILE_DIRECTORY.value + "$ownerId")

        if (!directory.exists() && !directory.mkdirs()) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Failed to create user directory"))
            return
        }

        val filePath = Paths.get(directory.absolutePath, fileName)

        val publicKey = getPublicKey(ownerId)

        if (contentLength != null && contentLength > Maximums.MAX_FILE_SIZE_BYTES.value) {
            call.respond(HttpStatusCode.BadRequest, "File too large (Max 1GB)")
        }

        if (publicKey != null) {

            try {
                val encryptedOutputStream = ByteArrayOutputStream()
                encryptFileStream(publicKey, filePath.inputStream(), encryptedOutputStream)
                withContext(Dispatchers.IO) {
                    Files.write(filePath, encryptedOutputStream.toByteArray())
                }
            } catch (e: Exception) {
                logger.error { "Error on file upload, deleting what was downloaded{$e.message}" }
                val file = File(filePath.toString())
                if (file.exists()) {
                    file.delete()
                }

            }

            call.respond(HttpStatusCode.OK, mapOf("Response" to "File Upload Success!"))
        } else {
            call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Invalid request parameters"))
        }

    } else {
        call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Invalid Request"))
    }
}

suspend fun fileDownload(call: ApplicationCall) {
    val ownerId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
    val parameters = call.parameters
    val fileID = parameters["fileId"]?.toIntOrNull()
    if (fileID != null) {
        val fileOwner = getOwnerId(fileID)
        val isOwner: Boolean = (fileOwner == ownerId)
        if (!isOwner) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("Response" to "You do not own this file"))
        }
        val fileMetaData: com.ironcrypt.database.File? = getFileData(fileID)
        val directory = File(Pathing.USER_FILE_DIRECTORY.value + "$fileMetaData.userID")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val filePath: Path? = Paths.get(directory.absolutePath, fileID.toString())
        if (filePath != null && Files.exists(filePath)) {
            withContext(Dispatchers.IO) {
                try {
                    Files.newInputStream(filePath).use { inputStream ->
                        call.respondOutputStream(ContentType.Application.OctetStream, HttpStatusCode.OK) {
                            inputStream.copyTo(this)
                        }
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Failed to download file"))
                }


            }

        } else call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Could not find file at specified path"))

    } else run {
        call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Invalid request parameters"))
    }

}

fun createUserDir(userID: Int): Boolean {
    val directoryPath = "/var/ironcrypt/$userID"

    val newUserDirectory = File(directoryPath)
    return newUserDirectory.mkdirs()
}


fun deleteUserDir(userID: Int) {
    val directoryPath = Paths.get("/var/ironcrypt/$userID")
    try {
        Files.walkFileTree(directoryPath, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                if (exc == null) {
                    Files.delete(dir)
                    return FileVisitResult.CONTINUE
                } else {
                    throw exc
                }
            }
        })
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

suspend fun fileDeletion(call: ApplicationCall) {
    val params = call.parameters
    val fileID: Int? = params["fileId"]?.toIntOrNull()
    val ownerId: Int? = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
    val filePath = Pathing.USER_FILE_DIRECTORY.value + "/$ownerId/$fileID"

    if (ownerId != null && fileID != null) {

        val fileOwner = getOwnerId(fileID)

        if (fileOwner != ownerId) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("Response" to "You do not own this file"))
            return
        }
        else{
            withContext(Dispatchers.IO) {
                try{
                    Files.delete(Path.of(filePath))
                    call.respond(HttpStatusCode.OK, mapOf("Response" to "File deleted successfully"))
                }catch (e:Exception){
                    logger.error { "Error deleting file, ${e.message}" }
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Error deleting file"))
                }

            }
        }

    }
}

suspend fun overLimit(inputStream: InputStream): Boolean {
    val byteLimit = Maximums.MAX_FILE_SIZE_BYTES.value
    var totalSize = 0
    val buffer = ByteArray(4096)

    var bytesRead: Int
    while (withContext(Dispatchers.IO) {
            inputStream.read(buffer)
        }.also { bytesRead = it } != -1) {
        totalSize += bytesRead
        if (totalSize > byteLimit) {
            return true


        }
    }

    return totalSize > byteLimit
}