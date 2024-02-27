package com.ironcrypt.database

import com.ironcrypt.enums.Maximums
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
    val ownerId: Int, val encryptedFile: ByteArray
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
    val spaceUsed = transaction {
        Files.select { Files.ownerId eq ownerId }.sumOf { it[Files.fileSizeBytes] }
    }

    return spaceUsed <= Maximums.MAX_USER_SPACE_BYTES.value
}

fun uploadFile(ownerId: Int, fileName: String, encryptedFile: ByteArray) {
    if (fileName.toCharArray().size > 255) {
        throw IllegalArgumentException()
    } else {
        if (verifyUsersSpace(ownerId)) {
            try {
                transaction {
                    Files.insert {
                        it[Files.ownerId] = ownerId
                        it[Files.fileName] = fileName
                        it[Files.encryptedFile] = ExposedBlob(encryptedFile)
                    }
                }
            } catch (e: Exception) {
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
    } catch (e: Exception) {
        logger.error { e }
    }
}

