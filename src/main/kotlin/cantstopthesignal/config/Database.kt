package cantstopthesignal.config

import cantstopthesignal.database.users.User
import cantstopthesignal.database.users.createUser
import cantstopthesignal.enums.Length
import cantstopthesignal.security.hashPassword
import cantstopthesignal.table_definitions.*
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File


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

    /*
        For context because I am using podman-compose in place of docker-compose, the normal health checks are a bit more annoying
        since there is no podman compose daemon doing health checks, I could create one but its easier to just do this. Try several times with
        a bit of a backoff instead of immediately failing when it cant connect because the container is still spinning up. If we were using
        docker-compose this wouldn't be an issue because of the daemon that does health checks.
     */
    val attempts = Length.MAX_DATABASE_RETRIES.value.toInt()

    var connected = false

    for (i in 1..attempts) {
        try {
            Database.connect(
                url = jdbcUrl,
                driver = "org.postgresql.Driver",
                user = dbUser,
                password = dbPassword
            )

            transaction {
                exec("SELECT 1")
            }

            println("Connected to database.")
            connected = true
            break
        } catch (e: Exception) {
            println("Database is not ready (${i}/$attempts): ${e.message}")
            val delayMs = i.toLong() * 5000 // bit of a backoff
            Thread.sleep(delayMs)
        }
    }

    if (!connected) {
        error("Database is not connected after $attempts attempts. Shutting down.")
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
        if (Users.selectAll().where(Users.isAdmin eq true).count().toInt() == 0) {
            val password = System.getenv("ADMIN_PASSWORD_FILE")
                ?.let { File(it).readText().trim() }
                ?: error("DATABASE_PASSWORD_FILE is required")

            val user: User = User(
                userName = "admin",
                passwordHash = password, // this is hashed inside so it shouldnt really be named passwordHash lol
                publicKey = null, // You Can set this in the web interface
                isAdmin = true,
                isModerator = false,
                isSuspended = false,
            )

            if (!createUser(user)) {
                error("Cannot create admin user!")
            }

        }
    }


}