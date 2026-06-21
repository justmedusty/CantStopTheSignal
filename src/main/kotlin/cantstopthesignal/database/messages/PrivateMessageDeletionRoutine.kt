package cantstopthesignal.database.messages

import cantstopthesignal.log.logger
import cantstopthesignal.table_definitions.Conversations
import cantstopthesignal.table_definitions.MessageNotifications
import cantstopthesignal.table_definitions.Messages
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction


private fun doWork() {
    return try {
        transaction {
            val applicableConversations =
                Conversations.selectAll().where { Conversations.transientMessages eq true }.map { it[Conversations.id] }
            if (applicableConversations.isEmpty()) {
                return@transaction
            }

            for (applicableConversation in applicableConversations) {
                val hasUnreadMessages = MessageNotifications.selectAll()
                    .where { MessageNotifications.conversationId eq applicableConversation }.count() > 0

                if (hasUnreadMessages) {
                    continue
                }

                Messages.deleteWhere { Messages.conversationId eq applicableConversation }


            }

        }
    } catch (e: Exception) {
        logger.error { "An error (${e.message}) occurred while trying to check if there is message deletion work to do." }

    }
}

fun doMessageDeletionRound() {
    doWork()
}