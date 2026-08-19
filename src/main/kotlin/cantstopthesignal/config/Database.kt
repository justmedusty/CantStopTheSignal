package cantstopthesignal.config

import cantstopthesignal.database.users.User
import cantstopthesignal.database.users.createUser
import cantstopthesignal.security.hashPassword
import cantstopthesignal.table_definitions.*
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import kotlin.system.exitProcess


/**
 * Configure database
 *
 */
fun Application.configureDatabase() {


    val dbHost = System.getenv("DATABASE_HOST") ?: "db"
    val dbPort = System.getenv("DATABASE_PORT") ?: "5432"
    val dbName = System.getenv("DATABASE_NAME") ?: "main"
    val dbUser = System.getenv("DATABASE_USER") ?: "postgres"
    val dbPassword = System.getenv("DATABASE_PASSWORD_FILE")

        ?.let { File(it).readText().trim() }
        ?: error("DATABASE_PASSWORD_FILE is required")
    val jdbcUrl = "jdbc:postgresql://$dbHost:$dbPort/$dbName"
    try {
        Database.connect(jdbcUrl, driver = "org.postgresql.Driver", user = dbUser, password = dbPassword)
    } catch (e: Exception) {
        e.printStackTrace()
        println(e)
        exitProcess(-1)
    }

    transaction {
        SchemaUtils.create(
            Users,
            Comments,
            Notifications, CommentEdits, CommentLikes, CommentDislikes, ProfileData, Posts,
            PostLikes,
            PostDislikes,
            PostContents,
            PostEdits,
            AdminLogs,
            SuspendLog,
            MessageNotifications,
            Messages,
            Conversations,
            ConversationMembers,
            SiteWidePermissions,
            PrivateMessageBlockList,
            InviteCodes // We'll create the table even if its not used by the person using this software
        )
    }

    transaction {
        if (Users.selectAll().count().toInt() == 0) {
            val password = System.getenv("ADMIN_PASSWORD")

                ?.let { File(it).readText().trim() }
                ?: error("DATABASE_PASSWORD_FILE is required")
            val user: User = User(
                userName = "admin",
                passwordHash = hashPassword(password),
                publicKey = null, // You Can set this in the web interface
                isAdmin = true,
                isModerator = false,
                isSuspended = false,
            )

            if (!createUser(user)){
                error("Cannot create admin user!")
            }

        }
    }


}