package cantstopthesignal.config

import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.AdminLogs
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.CommentDislikes
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.CommentEdits
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.CommentLikes
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.Comments
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.ConversationMembers
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.Conversations
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.InviteCodes
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.MessageNotifications
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.Messages
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.Notifications
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.PostContents
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.PostDislikes
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.PostEdits
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.PostLikes
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.Posts
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.PrivateMessageBlockList
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.ProfileData
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.SiteWidePermissions
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.SuspendLog
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.Users
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