package com.ironcrypt.fileio

import com.ironcrypt.database.getOwnerId
import com.ironcrypt.database.logger
import com.ironcrypt.enums.Maximums
import com.ironcrypt.enums.Pathing
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

private const val ERROR_ON_DELETION = "Error deleting file"
private const val DELETE_SUCCESS = "Success Deleting File"
fun createUserDir(userID: Int): Boolean {

    val directoryPath = Pathing.USER_FILE_DIRECTORY.value + userID.toString()
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
        } else {
            withContext(Dispatchers.IO) {
                try {
                    Files.delete(Path.of(filePath))
                    call.respond(HttpStatusCode.OK, mapOf("Response" to "File deleted successfully"))
                } catch (e: Exception) {
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