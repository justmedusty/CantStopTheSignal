package com.freedom.cantstopthesignal.database.dsl.table_definitions

import com.freedom.cantstopthesignal.database.dsl.table_definitions.PostDislikes.dislikedById
import com.freedom.cantstopthesignal.siteConfig
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import java.time.LocalDateTime


/****************************************************************************************************************************************************************
 * This file contains ALL the DSL table definitions, I prefer this project to have the DSL in one place to make it easier to add or check things
 ****************************************************************************************************************************************************************/
object Users : Table(name = "Users") {
    val id: Column<Long> = long("id").autoIncrement()
    val userName: Column<String> = varchar("user_name", 45).uniqueIndex()
    val passwordHash = text("password_hash").nullable() // Making this nullable in the case a user wishes to disable password login for better account security
    val isAdmin = bool("is_admin").default(false)
    val isModerator = bool("is_moderator").default(false)
    val isSuspended = bool("is_suspended").default(false)

    override val primaryKey = PrimaryKey(id)
}

object ProfileData : Table(name = "ProfileData") {
    val id: Column<Long> = long("id").autoIncrement()
    val userId: Column<Long> = long("user_id").references(Users.id, ReferenceOption.CASCADE)
    val bio = text("bio").nullable().default(null)
    val publicKey = text("public_key").nullable().default(null)
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val lastLogin: Column<LocalDateTime?> = datetime("last_login").nullable().default(null)

    override val primaryKey = PrimaryKey(id)
}

object Conversations : Table(name = "Conversations") {
    val id: Column<Long> = long("id").autoIncrement()
    val isGroup: Column<Boolean> = bool("is_group").default(false)
    val name: Column<String?> = varchar("name", 100).nullable()  // null for DMs
    val createdBy: Column<Long> = long("created_by").references(Users.id)
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

object ConversationMembers : Table(name = "ConversationMembers") {
    val conversationId: Column<Long> = long("conversation_id").references(Conversations.id)
    val userId: Column<Long> = long("user_id").references(Users.id)
    val joinedAt: Column<LocalDateTime> = datetime("joined_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(conversationId, userId)  // composite key
}


object Messages : Table(name = "Messages") {
    val id: Column<Long> = long("id").autoIncrement()
    val conversationId: Column<Long> = long("conversation_id").references(Conversations.id)
    val senderId: Column<Long> = long("sender_id").references(Users.id)
    val message: Column<String> = text("message")
    val timeSent: Column<LocalDateTime> = datetime("time_sent").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

object Notifications : Table(name = "Notifications") {
    val id: Column<Long> = long("id").autoIncrement()
    val read: Column<Boolean> = bool("read").default(false)
    val postId: Column<Long?> = long("post_id").references(Posts.id, onDelete = ReferenceOption.CASCADE).nullable()
    val commentId: Column<Long?> = long("comment_id").references(Comments.id).nullable().default(null)  // only if a comment reply
    val userId: Column<Long> = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val userWhoInteracted: Column<Long> = long("user_who_interacted").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val type: Column<Long> = long("type")

    override val primaryKey = PrimaryKey(id)
}


object MessageNotifications : Table(name = "MessageNotifications") {
    val id: Column<Long> = long("id").autoIncrement()
    val conversationId: Column<Long> = long("conversation_id").references(Messages.id, onDelete = ReferenceOption.CASCADE)
    val userId: Column<Long> = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(id)
}

object Posts : Table(name = "Posts") {
    val id: Column<Long> = long("id").autoIncrement()
    val posterId: Column<Long> = long("posterId").references(Users.id)
    val topic: Column<String> = varchar("topic", 60)
    val timestamp: Column<LocalDateTime> = datetime("timestamp").defaultExpression(CurrentDateTime)
    val deleted: Column<Boolean> = bool("deleted").default(false)
    val deletedReason: Column<Long?> = long("deleted_reason").nullable().default(null) /* soft deletion */



    override val primaryKey = PrimaryKey(id)
}

object PostLikes : Table(name = "Likes") {
    val id: Column<Long> = long("id").autoIncrement()
    val postId: Column<Long> = long("post_id").references(Posts.id, ReferenceOption.CASCADE)
    val likedById: Column<Long> = long("likedById").references(Users.id)


    init {
        index(true, postId, likedById)
    }
    override val primaryKey = PrimaryKey(id)
}

object PostEdits : Table(name = "PostEdits") {
    val id: Column<Long> = long("id").autoIncrement()
    val postId: Column<Long> = long("commentId").references(Posts.id)
    val lastEdited: Column<LocalDateTime> = datetime("lastEdited")


    override val primaryKey = PrimaryKey(id)
}

object PostDislikes : Table(name = "Dislikes") {
    val id: Column<Long> = long("id").autoIncrement()
    val postId: Column<Long> = long("post").references(Posts.id, ReferenceOption.CASCADE)
    val dislikedById: Column<Long> = long("dislikedBy").references(Users.id)

    init {
        index(true, postId, dislikedById)
    }

    override val primaryKey = PrimaryKey(id)
}

object PostContents : Table(name = "PostContents") {
    val id: Column<Long> = long("id").autoIncrement()
    var title: Column<String> = text("title")
    val content: Column<String> = text("postContent")
    val postId: Column<Long> = long("post").references(Posts.id, onDelete = ReferenceOption.CASCADE)


    override val primaryKey = PrimaryKey(id)
}

object Comments : Table(name = "Comments") {
    val id: Column<Long> = long("id").autoIncrement()
    val content: Column<String> = text("commentContent")
    val postId: Column<Long> = long("post").references(Posts.id, ReferenceOption.CASCADE)
    val commenterId: Column<Long> = long("commenterId").references(Users.id)
    val isReply: Column<Boolean> = bool("isReply").default(false)
    val parentCommentId: Column<Long?> =
        long("parentCommentId").references(id, onDelete = ReferenceOption.CASCADE).nullable().default(null)
    val timeStamp: Column<LocalDateTime> = datetime("time_posted").defaultExpression(CurrentDateTime)
    val deleted: Column<Boolean> = bool("deleted").default(false)
    val deletedReason: Column<Long?> = long("deleted_reason").nullable().default(null) /* soft deletion */


    override val primaryKey = PrimaryKey(id)
}

object CommentLikes : Table(name = "CommentLikes") {
    val id: Column<Long> = long("id").autoIncrement()
    val commentId: Column<Long> = long("commentId").references(Comments.id, ReferenceOption.CASCADE)
    val likedById: Column<Long> = long("likedById").references(Users.id)

    init {
        index(true, commentId, likedById)
    }

    override val primaryKey = PrimaryKey(id)
}

object CommentEdits : Table(name = "CommentEdits") {
    val id: Column<Long> = long("id").autoIncrement()
    val commentId: Column<Long> = long("commentId").references(Comments.id)
    val posterId: Column<Long> = long("posterId").references(Users.id)
    val lastEdited: Column<LocalDateTime> = datetime("lastEdited")


    override val primaryKey = PrimaryKey(id)
}


object CommentDislikes : Table(name = "CommentDislikes") {
    val id: Column<Long> = long("id").autoIncrement()
    val commentId: Column<Long> = long("commentId").references(
        Comments.id, ReferenceOption.CASCADE
    )
    val dislikedById: Column<Long> = long("dislikedById").references(Users.id)


    init {
        index(true, commentId, dislikedById)
    }


    override val primaryKey = PrimaryKey(id)
}

object AdminLogs : Table(name = "AdminLogs") {
    val id: Column<Long> = long("id").autoIncrement()
    val timestamp: Column<LocalDateTime> = datetime("timestamp")
    val doneById: Column<Long> = long("done_by_id").references(Users.id)
    val actionString: Column<String> = text("action_string") // I will just construct an action string this will be for things like deleting someones post or comment etc
    //cont. ^ I would tie it to ids but if a post is removed then there will no longer be an id to link to. This is mostly so that admins can just do whatever other staff can see what theyre doing
    val reason: Column<String> = text("reason")

    override val primaryKey = PrimaryKey(id)
}

object SuspendLog : Table(name = "SuspendLog") {
    val id: Column<Long> = long("id").autoIncrement()
    val timestamp: Column<LocalDateTime> = datetime("suspend_time")
    val adminId: Column<Long> = long("admin_id").references(Users.id)
    val suspendedUserId: Column<Long> = long("suspended_user_id").references(Users.id)
    val reason: Column<String> = text("reason")
    override val primaryKey = PrimaryKey(id)
}
//my idea for creating this table is something that can be done by admins dynamically to create an entry that essentially turns off account creation,
// this can be useful if someone wants to suddenly make their forum private or if they are being bombarded by signups that they do not want
object SiteWidePermissions : Table(name = "SiteWidePermissions") {
    val id: Column<Long> = long("id").autoIncrement()
    val timestamp: Column<LocalDateTime> = datetime("timestamp")
    val eventId: Column<Long> = long("event_id").uniqueIndex() //this will be used for nuclear options in case you get swarmed or something, but it can be used to include other stuff too
}

//If the user chooses to make their forum invite only, this will be where the one-time-use codes are stored
object InviteCodes : Table(name = "InviteCodes") {
    val id: Column<Long> = long("id").autoIncrement()
    val inviteCode: Column<String> = text("invite_code").uniqueIndex()
}

