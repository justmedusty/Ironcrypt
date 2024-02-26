package com.ironcrypt.configuration

import com.ironcrypt.database.Files
import com.ironcrypt.database.Users
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    val url = System.getenv("POSTGRES_URL")
    val user = System.getenv("POSTGRES_USER")
    val password = System.getenv("POSTGRES_PASSWORD")

    try {
        Database.connect(url, driver = "org.postgresql.Driver", user = user, password = password)
    } catch (e: Exception) {
        println(e)
    }

    transaction {
        SchemaUtils.create(Users, Files)
    }
}
