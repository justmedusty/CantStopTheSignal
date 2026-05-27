package cantstopthesignal.config

import com.freedom.cantstopthesignal.database.dsl.table_definitions.AdminLogs
import com.freedom.cantstopthesignal.database.dsl.table_definitions.CommentDislikes
import com.freedom.cantstopthesignal.database.dsl.table_definitions.CommentEdits
import com.freedom.cantstopthesignal.database.dsl.table_definitions.CommentLikes
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Comments
import com.freedom.cantstopthesignal.database.dsl.table_definitions.GroupMemberships
import com.freedom.cantstopthesignal.database.dsl.table_definitions.GroupMessages
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Groups
import com.freedom.cantstopthesignal.database.dsl.table_definitions.InviteCodes
import com.freedom.cantstopthesignal.database.dsl.table_definitions.MessageNotifications
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Notifications
import com.freedom.cantstopthesignal.database.dsl.table_definitions.PostContents
import com.freedom.cantstopthesignal.database.dsl.table_definitions.PostDislikes
import com.freedom.cantstopthesignal.database.dsl.table_definitions.PostEdits
import com.freedom.cantstopthesignal.database.dsl.table_definitions.PostLikes
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Posts
import com.freedom.cantstopthesignal.database.dsl.table_definitions.ProfileData
import com.freedom.cantstopthesignal.database.dsl.table_definitions.SiteWidePermissions
import com.freedom.cantstopthesignal.database.dsl.table_definitions.SuspendLog
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Users
import io.ktor.server.application.Application
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
            Notifications
            , CommentEdits
            , CommentLikes
            , CommentDislikes
            , ProfileData
            , Posts,
            PostLikes,
            PostDislikes,
            PostContents,
            PostEdits,
            AdminLogs,
            SuspendLog,
            MessageNotifications,
            Messages,
            GroupMessages,
            Groups,
            GroupMemberships,
            SiteWidePermissions,
            InviteCodes // We'll create the table even if its not used by the person using this software
        )
    }
}