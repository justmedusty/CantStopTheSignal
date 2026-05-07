import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Notifications
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

data class Notification(
    val id: Long,
    val read: Boolean,
    val eventId: Long,
    val user: Long,
    val type: Long
)

fun insertNotification(id: Long, user: Long, notifType: Long): Boolean {
    return try {
        transaction {
            Notifications.insert {
                it[eventId] = id
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

fun getAllNotifications(page: Long, limit: Long, userId: Long): List<Notification>? {
    return try {
        transaction {
            Notifications.select(Notifications.userId eq userId).limit(limit.toInt()).offset((page - 1) * limit).map {
                Notification(
                    it[Notifications.id],
                    it[Notifications.read],
                    it[Notifications.eventId],
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

fun markNotifRead(notif: Long,user: Long): Boolean {

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

fun markNotifUnread(notif: Long,user: Long): Boolean {

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
