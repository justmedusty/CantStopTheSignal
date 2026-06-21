package cantstopthesignal.database.notifications

import cantstopthesignal.log.logger
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.MessageNotifications
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

data class MessagesNotification(
    val message: Long,
    val read: Boolean,

    )

fun insertMessageNotification(message: Long, user: Long): Boolean {
    return try {
        transaction {
            MessageNotifications.insert {
                it[conversationId] = message
                it[userId] = user
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }

}

fun numUnreadMessages(user: Long): Long {
    return try {
        transaction {
            MessageNotifications.selectAll().where { MessageNotifications.userId eq user }.count()
        }
    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to fetch unread messages" }
        0
    }
}

fun numUnreadMessagesInConversation(user: Long, conversationId: Long): Long {
    return try {
        transaction {
            MessageNotifications.selectAll().where { (MessageNotifications.userId eq user) and (MessageNotifications.conversationId eq conversationId) }.count()
        }
    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to fetch unread messages" }
        0
    }
}