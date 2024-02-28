package com.ironcrypt.fileio

import com.ironcrypt.database.getFileData
import com.ironcrypt.database.getPublicKey
import com.ironcrypt.encryption.encryptFileStream
import com.ironcrypt.enums.Maximums
import com.ironcrypt.enums.Pathing
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
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
            createUserDir(userID)
        }

        val filePath = Paths.get(directory.absolutePath, fileName)
        val publicKey = getPublicKey(userID)

        if (publicKey != null) {
            val encryptedOutputStream = ByteArrayOutputStream()
            encryptFileStream(publicKey, filePath.inputStream(), encryptedOutputStream)
            withContext(Dispatchers.IO) {
                Files.write(filePath, encryptedOutputStream.toByteArray())

            }
            call.respond(HttpStatusCode.OK, mapOf("Response" to "File Upload Success!"))
        } else {
            call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Invalid request parameters"))
        }
    }
}

suspend fun fileDownload(call: ApplicationCall) {
    val parameters = call.parameters
    val fileID = parameters["fileId"]?.toIntOrNull()
    if (fileID != null) {
        val fileMetaData: com.ironcrypt.database.File? = getFileData(fileID)
        val directory = File(Pathing.USER_FILE_DIRECTORY.value + "$fileMetaData.userID")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val filePath: Path? = Paths.get(directory.absolutePath, fileID.toString())
        if (filePath != null && Files.exists(filePath)) {
            withContext(Dispatchers.IO) {

                Files.newInputStream(filePath).use { inputStream ->
                    call.respondOutputStream(ContentType.Application.OctetStream, HttpStatusCode.OK) {
                        inputStream.copyTo(this)
                    }
                }
            }

        } else call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Could not find file at specified path"))

    } else run {
        call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Invalid request parameters"))
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


suspend fun overLimit(inputStream: InputStream): Boolean {
    val byteLimit = Maximums.MAX_FILE_SIZE_BYTES.value
    var totalSize = 0
    val buffer = ByteArray(4096)

    var bytesRead: Int
    while (withContext(Dispatchers.IO) {
            inputStream.read(buffer)
        }.also { bytesRead = it } != -1) {
        totalSize += bytesRead
       if (totalSize > byteLimit){
         return true


       }
    }

    return totalSize > byteLimit
}