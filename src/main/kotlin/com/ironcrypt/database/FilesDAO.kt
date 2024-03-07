package com.ironcrypt.database

import com.ironcrypt.enums.Maximums
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object Files : Table(name = "Files") {
    val id: Column<Int> = integer("id").autoIncrement()
    val ownerId: Column<Int> = integer("owner_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val fileName: Column<String> = varchar("fileName", 500)
    val fileSizeBytes: Column<Long> = long("file_size_bytes")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(fileName, ownerId)
    }
}

data class File(
    val fileId: Int, val ownerId: Int, val fileName: String, val fileSizeBytes: Long
)

fun verifyUsersSpace(ownerId: Int): Boolean {
    try {
        val spaceUsed = transaction {
            Files.select { Files.ownerId eq ownerId }.sumOf { it[Files.fileSizeBytes] }
        }
        logger.error { spaceUsed <= Maximums.MAX_USER_SPACE_BYTES.value }
        return spaceUsed <= Maximums.MAX_USER_SPACE_BYTES.value

    } catch (e: ExposedSQLException) {
        return false
    }

}

suspend fun addFileData(ownerId: Int, fileName: String, fileSizeBytes: Long, call: ApplicationCall) {
    if (fileName.toCharArray().size > 500) {
        logger.error { "Exceeded maximum filesize or file name length" }
        throw IllegalArgumentException("Exceeded maximum filesize or file name length")
    }

    if (!verifyUsersSpace(ownerId)) {
        logger.error { "User out of space" }
        call.respond(
            HttpStatusCode.InsufficientStorage, mapOf("Response" to "You have reached your maximum allowed file space")
        )
    } else {
        try {
            transaction {
                Files.insert {
                    it[Files.ownerId] = ownerId
                    it[Files.fileName] = fileName
                    it[Files.fileSizeBytes] = fileSizeBytes
                }
            }
        } catch (e: ExposedSQLException) {
            logger.error { e }
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("Response" to "Could not be inserted into database, is this a duplicate?")
            )
        }
    }


}

fun getFileId(fileName: String, fileSizeBytes: Long): Int? {
    return try {
        transaction {
            // Make sure to select the fileId from the table
            Files.select {
                (Files.fileName eq fileName) and (Files.fileSizeBytes eq fileSizeBytes)
            }.singleOrNull() // Using singleOrNull to handle cases where no record is found
                ?.get(Files.id)
        }
    } catch (e: ExposedSQLException) {
        logger.error { e }
        null
    }
}

fun deleteFile(fileId: Int) {
    try {
        transaction {
            Files.deleteWhere {
                id eq fileId
            }
        }
    } catch (e: ExposedSQLException) {
        logger.error { "Error deleting file : ${e.message}" }
    }
}

fun getOwnerId(fileId: Int): Int? {
    try {
        val ownerId = transaction {
            Files.select { Files.id eq fileId }.singleOrNull()?.get(Files.ownerId)
        }
        if (ownerId != null) {
            return ownerId
        }
        return null
    } catch (e: ExposedSQLException) {
        logger.error { "Error getting ownerId from checking File Table :  ${e.message}" }
        return null
    }


}


fun getAllFiles(ownerId: Int, limit: Int, page: Int): List<File>? {
    val offset: Long = ((page - 1) * limit).toLong()
    return try {
        transaction {
            Files.select { Files.ownerId eq ownerId }.limit(limit, offset).orderBy(Files.id to SortOrder.DESC).map {
                    File(
                        it[Files.id], it[Files.ownerId], it[Files.fileName], it[Files.fileSizeBytes]
                    )
                }
        }
    } catch (e: ExposedSQLException) {
        logger.error { "Error fetching files ${e.message}" }
        return null
    }


}

fun getFileData(fileId: Int): File? {
    return try {
        transaction {
            Files.select { Files.id eq fileId }.singleOrNull()?.let { row: ResultRow ->
                File(
                    row[Files.id], row[Files.ownerId], row[Files.fileName], row[Files.fileSizeBytes]
                )
            }
        }
    } catch (e: ExposedSQLException) {
        logger.error { "Error getting file data : {${e.message}" }
        return null
    }
}
