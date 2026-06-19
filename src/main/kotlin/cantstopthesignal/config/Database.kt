package cantstopthesignal.config

import com.freedom.cantstopthesignal.database.dsl.table_definitions.*
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.system.exitProcess


/**
 * Configure database
 *
 */
fun Application.configureDatabase() {
    val url = System.getenv("POSTGRES_URL")
    val user = System.getenv("POSTGRES_USER")
    val password = System.getenv("POSTGRES_PASSWORD")

    try {
        Database.connect(url, driver = "org.postgresql.Driver", user = user, password = password)
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
}