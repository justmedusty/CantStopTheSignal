package cantstopthesignal.database.messages

import cantstopthesignal.database.users.*
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.ConversationMembers
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Conversations
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages.message
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages.receiverId
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages.senderId
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages.timeSent
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime
import java.time.ZoneOffset


data class Message(
    val id: Long,
    val senderId: Long,
    val receiverId: Long,
    val message: String,
    val timeSent: LocalDateTime
)

//This is the high level object
data class MessageObject(
    val id: Long,
    val sender: String,
    val isMe: Boolean,
    val message: String,
    val timeSent: LocalDateTime,
)

data class MessageConversationObject(
    val id: Long,
    val lastSender: String,
    val isMe: Boolean,
    val timeOfLastMessage: LocalDateTime,
    val members: List<String>,
    val pgpKey: List<String>? //This is just here to prompt users to encrypt their own messages with the convo members uploaded IDs,
)


fun sendMessage(sender: Long, conversation: Long, messageString: String): Long? {

    return try {

        transaction {

            Messages.insert {
                it[senderId] = sender
                it[conversationId] = conversation
                it[message] = messageString
                it[timeSent] = LocalDateTime.now(ZoneOffset.UTC)
            }


        } get Messages.id

    } catch (e: Exception) {
        logger.error { e.message }
        null
    }

}

fun getMessagesFromConversation(
    requesterId: Long,
    conversationId: Long,
    page: Int,
    limit: Int
): List<MessageObject>? {

    val offsetVal = ((page - 1) * limit).toLong()


    return try {

        transaction {
            val messageList = Messages
                .selectAll()
                .where { Messages.conversationId eq conversationId }
                .limit(limit)
                .offset(offsetVal)
                .orderBy(timeSent to SortOrder.DESC)
                .map {
                    MessageObject(
                        id = it[Messages.id],
                        sender = getUserNameWithinTransaction(it[senderId])!!, // This should not conceivably be null so we will force this for now
                        message = it[message],
                        timeSent = it[timeSent],
                        isMe = it[senderId] == requesterId
                    )
                }

            messageList
        }
    } catch (e: Exception) {
        logger.error { e.message }
        return null //return null on error , not empty list, because empty list can mean more than just error, null can only mean error
    }
}

//This should only be called within a tx
fun getMembersOfConversation(conversation: Long): List<String>? {
    return try {
        val usernameList: List<String> =
            ConversationMembers.selectAll().where { ConversationMembers.conversationId eq conversation }.map {
                getUserNameWithinTransaction(it[ConversationMembers.userId])!! //There should not be any null entries here we will verify that the conversation actually exists somewhere
            }

        usernameList

    } catch (e: Exception) {
        logger.error { e.message + " Happened trying to get members of conversation" }
        null
    }
}

fun getAllConversations(userId: Long, page: Int, limit: Int): List<MessageConversationObject>? {
    val offsetVal = ((page - 1) * limit).toLong()
    val receiverUserNameString = getUserName(userId)
    return if (receiverUserNameString != null) {
        try {
            transaction {

                Conversations.selectAll().where { (r eq userId) }.limit(limit).offset(offsetVal).map {

                    MessageConversationObject(
                        id = it[Messages.id],
                        sender = getUserNameWithinTransaction(it[Conversations.])!!,
                        message = it[message],
                        timeSent = it[timeSent]


                    )
                }
            }
        } catch (e: Exception) {
            logger.error { e.message }
            null
        }
    } else return null
}

fun getLastMessageTimestamp(userId: Long, receiver: Long): LocalDateTime? {
    return try {
        transaction {
            Messages.selectAll().where { (receiverId eq receiver) and (senderId eq userId) }.orderBy(
                Messages.id,
                SortOrder.DESC
            ).map { it[timeSent] }.first()
        }
    } catch (e: Exception) {
        logger.error { e.message + "Error occurred trying to fetch the last message time for a user" }
        null
    }
}

