package com.ironcrypt.fileio

import com.ironcrypt.database.getPublicKey
import com.ironcrypt.encryption.encryptFileStream
import com.ironcrypt.enums.Pathing
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.utils.io.errors.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.inputStream

fun fileDownload() {

}

suspend fun fileUpload(call: ApplicationCall) {
    val parameters = call.receiveMultipart().readAllParts().associateBy({ it.name ?: "" }, { it })

    val filePart = parameters["file"]
    val fileName = parameters["fileName"]?.let { part -> (part as? PartData.FormItem)?.value }
    val userID = parameters["userID"]?.let { part -> (part as? PartData.FormItem)?.value }?.toIntOrNull()


    if (filePart != null && fileName != null && userID != null) {
        val directory = File(Pathing.USER_FILE_DIRECTORY.value + "$userID")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val filePath = Paths.get(directory.absolutePath, fileName)
        val publicKey = getPublicKey(userID)

        if (publicKey != null) {
            val encryptedOutputStream = ByteArrayOutputStream()
            encryptFileStream(publicKey, filePath.inputStream(), encryptedOutputStream)
            call.respondBytes(encryptedOutputStream.toByteArray(), ContentType.Application.OctetStream)
        } else {
            call.respond(HttpStatusCode.BadRequest, "Invalid request parameters")
        }
    }
}

fun createUserDir(userID: Int) {
    val directoryPath = "/var/ironcrypt/$userID"

    val newUserDirectory = File(directoryPath)
    newUserDirectory.mkdirs()
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
