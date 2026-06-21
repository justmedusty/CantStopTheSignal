package cantstopthesignal.config

import cantstopthesignal.table_definitions.AdminLogs
import cantstopthesignal.table_definitions.CommentDislikes
import cantstopthesignal.table_definitions.CommentEdits
import cantstopthesignal.table_definitions.CommentLikes
import cantstopthesignal.table_definitions.Comments
import cantstopthesignal.table_definitions.ConversationMembers
import cantstopthesignal.table_definitions.Conversations
import cantstopthesignal.table_definitions.InviteCodes
import cantstopthesignal.table_definitions.MessageNotifications
import cantstopthesignal.table_definitions.Messages
import cantstopthesignal.table_definitions.Notifications
import cantstopthesignal.table_definitions.PostContents
import cantstopthesignal.table_definitions.PostDislikes
import cantstopthesignal.table_definitions.PostEdits
import cantstopthesignal.table_definitions.PostLikes
import cantstopthesignal.table_definitions.Posts
import cantstopthesignal.table_definitions.PrivateMessageBlockList
import cantstopthesignal.table_definitions.ProfileData
import cantstopthesignal.table_definitions.SiteWidePermissions
import cantstopthesignal.table_definitions.SuspendLog
import cantstopthesignal.table_definitions.Users
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