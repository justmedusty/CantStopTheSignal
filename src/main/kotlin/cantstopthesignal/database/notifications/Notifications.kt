package cantstopthesignal.database.notifications

import cantstopthesignal.database.users.getUserName
import cantstopthesignal.log.logger
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.Notifications
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.math.ceil

data class Notification(
    val id: Long,
    val read: Boolean,
    val postId: Long?,
    val commentId: Long?,
    val user: Long,
    val usernameWhoInteracted: String,
    val type: Long,
    val numPages: Long //Again, duplicates things but its easier to m'

)

fun doesNotificationAlreadyExist(
    post: Long?,
    comment: Long?,
    user: Long,
    userWhoInteracted: Long,
    notifType: Long
): Boolean {
    if ((user == userWhoInteracted)) {
        /*
            I am more worried about someone unliking and liking something getting multiple notifications, multiple replies or comments i think should
            get notifications pushed

            We are also going to skip notifs for liking or commenting on your own shit cause you dont need a notif for that
          */
        return true
    }
    return try {
        transaction {
            val count = Notifications.selectAll()
                .where { (Notifications.postId eq post) and (Notifications.userWhoInteracted eq userWhoInteracted) and (Notifications.commentId eq comment) and (Notifications.userId eq user) and (Notifications.type eq notifType) }
                .count()

            count > 0
        }
    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to check if a notification already exists" }
        true
    }
}

fun insertNotification(
    postId: Long?,
    commentId: Long?,
    user: Long,
    userWhoInteracted: Long,
    notifType: Long
): Boolean? {
    if (postId == null && commentId == null) return false
    return try {
        transaction {
            if (doesNotificationAlreadyExist(postId, commentId, user, userWhoInteracted, notifType)) {
                logger.info { "Notification already exists post $postId comment $commentId user $user userwhointeracted $userWhoInteracted notiftype $notifType" }
                return@transaction false
            }
            Notifications.insert {
                it[Notifications.postId] = postId
                it[Notifications.commentId] = commentId
                it[Notifications.userWhoInteracted] = userWhoInteracted
                it[Notifications.userId] = user
                it[Notifications.type] = notifType
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }

}

fun getAllNotifications(page: Long, limit: Long, userId: Long): List<Notification>? {
    return try {
        transaction {
            val numPages = ceil(
                Notifications.selectAll().where { Notifications.userId eq userId }.count()
                    .toDouble() / limit.toDouble()
            ).toLong()
            Notifications.selectAll()
                .where { Notifications.userId eq userId }
                .orderBy(Notifications.id, SortOrder.DESC)  // sort in DB, before pagination
                .limit(limit.toInt())
                .offset((page - 1) * limit)
                .map {

                    Notification(
                        it[Notifications.id],
                        it[Notifications.read],
                        it[Notifications.postId],
                        it[Notifications.commentId],
                        userId,
                        getUserName(it[Notifications.userWhoInteracted]) ?: "deleted user",
                        it[Notifications.type],
                        numPages
                    )
                }

        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

fun markNotifRead(notif: Long, user: Long): Boolean {

    return try {
        transaction {
            Notifications.update({ (Notifications.id eq notif) and (Notifications.userId eq user) }) {
                it[read] = true
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun markNotifUnread(notif: Long, user: Long): Boolean {

    return try {
        transaction {
            Notifications.update({ (Notifications.id eq notif) and (Notifications.userId eq user) }) {
                it[read] = false
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun markAllNotificationsRead(userId: Long): Boolean {
    return try {
        transaction {
            Notifications.update(where = { Notifications.userId eq userId }) {
                it[Notifications.read] = true
            }
        } > 0  // returns true if at least 1 row was updated
    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to mark all notifications read" }
        false
    }
}

fun getUnreadNotificationsCount(user: Long): Long {
    return try {
        transaction {
            Notifications.selectAll().where((Notifications.userId eq user) and (Notifications.read eq false)).count()
        }
    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to fetch unread notifs" }
        0
    }
}

fun deleteAllNotifications(user: Long): Boolean {
    return try {
        Notifications.deleteWhere { Notifications.userId eq user } > 0
    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to delete all notifs" }
        false
    }
}