import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table


object Users : Table(name = "Users") {
    val id: Column<Int> = integer("id").autoIncrement()
    val userName: Column<String> = varchar("user_name", 45).uniqueIndex()
    val publicKey: Column<String?> = text("public_key").uniqueIndex().nullable()
    val passwordHash = text("password_hash")

    override val primaryKey = PrimaryKey(id)
}
