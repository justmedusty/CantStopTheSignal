package cantstopthesignal.database.notifications

import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Notifications
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

data class Notification(
    val id: Long,
    val read: Boolean,
    val postId: Long?,
    val commentId: Long?,
    val user: Long,
    val type: Long
)


fun insertNotification(postId: Long?, commentId: Long?, user: Long, notifType: Long): Boolean {
    if (postId == null && commentId == null) return false
    return try {
        Notifications.insert {
            it[Notifications.postId] = postId
            it[Notifications.commentId] = commentId
            it[userId] = user
            it[type] = notifType
        }
        true
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }

}

fun getAllNotifications(page: Long, limit: Long, userId: Long): List<Notification>? {
    return try {
        transaction {
            Notifications.selectAll().where { (Notifications.userId eq userId) }.limit(limit.toInt())
                .offset((page - 1) * limit).sortedByDescending { it[Notifications.id] }.map {
                    Notification(
                        it[Notifications.id],
                        it[Notifications.read],
                        it[Notifications.postId],
                        it[Notifications.commentId],
                        userId,
                        it[Notifications.type]
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