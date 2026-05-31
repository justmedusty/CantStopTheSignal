import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Notifications
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
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

fun insertNotification(id: Long, user: Long, notifType: Long): Boolean {
    return try {
        transaction {
            Notifications.insert {
                it[Notifications.postId] = id
                it[userId] = user
                it[type] = notifType
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }

}

fun insertNotificationWithinTransaction(postId: Long?, commentId: Long?, user: Long, notifType: Long): Boolean {
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
