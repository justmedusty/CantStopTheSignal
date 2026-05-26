import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.MessageNotifications
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

data class MessagesNotification(
    val message: Long,
    val read: Boolean,

    )

fun insertMessageNotification(message: Long, user: Long): Boolean {
    return try {
        transaction {
            MessageNotifications.insert {
                it[messageId] = message
                it[userId] = user
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }

}

