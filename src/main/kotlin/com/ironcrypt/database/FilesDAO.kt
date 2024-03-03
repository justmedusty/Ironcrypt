package com.ironcrypt.database

import com.ironcrypt.enums.Maximums
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object Files : Table(name = "Files") {
    val id: Column<Int> = integer("id").autoIncrement()
    val ownerId: Column<Int> = integer("owner_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val fileName: Column<String> = varchar("fileName", 500)
    val fileSizeBytes: Column<Int> = integer("file_size_bytes")

    override val primaryKey = PrimaryKey(id)

}

data class File(
    val fileId: Int, val ownerId: Int, val fileName: String, val fileSizeBytes: Int
)

fun verifyUsersSpace(ownerId: Int): Boolean {
    try {
        val spaceUsed = transaction {
            Files.select { Files.ownerId eq ownerId }.sumOf { it[Files.fileSizeBytes] }
        }

        return spaceUsed <= Maximums.MAX_USER_SPACE_BYTES.value
    } catch (e: ExposedSQLException) {
        return false
    }

}

fun addFileData(ownerId: Int, fileName: String, fileSizeBytes: Int){
    if (fileName.toCharArray().size > 500) {
        logger.error { "Exceeded maximum filesize or file name length" }
        throw IllegalArgumentException("Exceeded maximum filesize or file name length")
    } else {
        if (verifyUsersSpace(ownerId)) {
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
            }
        } else {
            logger.error { "Invalid user credentials given" }
            throw IllegalArgumentException()
        }
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


fun getAllFiles(ownerId: Int): List<File>? {
    return try {
        transaction {
            Files.select { Files.ownerId eq ownerId }.map {
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

