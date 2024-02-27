package com.ironcrypt.database

import com.ironcrypt.enums.Maximums
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction

object Files : Table(name = "Files") {
    val id: Column<Int> = integer("id").autoIncrement()
    val ownerId: Column<Int> = integer("owner_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val fileName: Column<String> = varchar("fileName", 255)
    val encryptedFile: Column<ExposedBlob> = blob("encrypted_file")
    val fileSizeBytes: Column<Int> = integer("file_size_bytes")

    override val primaryKey = PrimaryKey(id)

}

data class File(
    val ownerId: Int, val encryptedFile: ByteArray, val fileName: String, val fileSizeBytes: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as File

        if (ownerId != other.ownerId) return false
        if (!encryptedFile.contentEquals(other.encryptedFile)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ownerId
        result = 31 * result + encryptedFile.contentHashCode()
        return result
    }
}

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

fun uploadFile(ownerId: Int, fileName: String, encryptedFile: ByteArray) {
    if (fileName.toCharArray().size > 255 || encryptedFile.size > Maximums.MAX_FILE_SIZE_BYTES.value) {
        logger.error { "Exceeded maximum filesize or file name length" }
        throw IllegalArgumentException("Exceeded maximum filesize or file name length")
    } else {
        if (verifyUsersSpace(ownerId)) {
            val fileSizeBytes = encryptedFile.size
            try {
                transaction {
                    Files.insert {
                        it[Files.ownerId] = ownerId
                        it[Files.fileName] = fileName
                        it[Files.encryptedFile] = ExposedBlob(encryptedFile)
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

fun downloadFile(fileId: Int): ByteArray? {
    try {
        return transaction {
            Files.select { Files.id eq fileId }.singleOrNull()

        }?.let { row ->
            row[Files.encryptedFile].bytes
        }

    } catch (e: ExposedSQLException) {
        logger.error { "Error getting file from fileId : ${e.message}" }
        return null
    }

}

fun getAllFiles(ownerId: Int): List<File>? {
    return try {
        transaction {
            Files.select { Files.ownerId eq ownerId }.map {
                File(
                    it[Files.ownerId], encryptedFile = ByteArray(0), it[Files.fileName], it[Files.fileSizeBytes]
                )
            }
        }
    } catch (e: ExposedSQLException) {
        logger.error { "Error fetching files ${e.message}" }
        return null
    }


}

