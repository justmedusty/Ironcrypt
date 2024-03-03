package com.ironcrypt.fileio

import com.ironcrypt.enums.Pathing
import io.ktor.utils.io.errors.*
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes


fun createUserDir(userID: Int): Boolean {

    val directoryPath = Pathing.USER_FILE_DIRECTORY.value + userID.toString()
    val newUserDirectory = File(directoryPath)
    return newUserDirectory.mkdirs()
}


fun deleteUserDir(userID: Int) {
    val directoryPath = Paths.get(Pathing.USER_FILE_DIRECTORY.value+ "$userID")
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
