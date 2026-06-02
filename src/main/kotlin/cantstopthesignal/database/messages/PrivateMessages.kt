package cantstopthesignal.database.messages

import cantstopthesignal.database.users.User
import cantstopthesignal.database.users.getPublicKey
import cantstopthesignal.database.users.getUserIdWithinTransaction
import cantstopthesignal.database.users.getUserName
import cantstopthesignal.database.users.getUserNameWithinTransaction
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.ConversationMembers
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Conversations
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages.message
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages.senderId
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages.timeSent
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
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

fun getLastMessageUsername(conversation: Long): String? {
    return try {
        val userId = Messages.selectAll().where { Messages.conversationId eq conversation }
            .orderBy(Messages.id to SortOrder.DESC)
            .first()[senderId]

        getUserNameWithinTransaction(userId)

    } catch (e: Exception) {
        logger.error { e.message + " occurred while trying to get last username " }
        null
    }
}

fun getUsersInConversation(userId: Long, groupId: Long): List<String>? {

    return try {
        val list = ConversationMembers.selectAll()
            .where { (ConversationMembers.conversationId eq groupId) and (ConversationMembers.userId neq userId) }.map {
                getUserNameWithinTransaction(it[ConversationMembers.userId])
            }
        for (item in list) {
            if (item == null) {
                /* This isn't the end of the world but we should know about it if it happens , it should not happen */
                logger.warn { "getUsersInConversation: There is a null username tied to a userId in a message conversation" }
                return null
            }
        }
        list
    } catch (e: Exception) {
        logger.error { e.message + " happened while trying to fetch users in conversation" }
        null
    } as List<String>?

}


fun getPgpKeysInConversation(userId: Long, groupId: Long): List<String>? {

    return try {
        val list = ConversationMembers.selectAll()
            .where { (ConversationMembers.conversationId eq groupId) and (ConversationMembers.userId neq userId) }.map {
                getPublicKey(it[ConversationMembers.userId])

            }
        val keyList: List<String> = list.filterNotNull().map { it }
        keyList
    } catch (e: Exception) {
        logger.error { e.message + " happened while trying to fetch users pgp keys in conversation" }
        null
    }

}

fun verifyConversationId(id: Long): Long? {
    return try {
        transaction {
            Conversations.selectAll().where { Conversations.id eq id }.limit(1).first()[Conversations.id]
        }
    } catch (e: Exception) {
        logger.error { e.message + " occurred while trying to verify conversation ID" }
        null
    }
}

fun getAllConversations(userId: Long, page: Int, limit: Int): List<MessageConversationObject>? {
    val offsetVal = ((page - 1) * limit).toLong()
    val receiverUserNameString = getUserName(userId)
    return if (receiverUserNameString != null) {
        try {
            transaction {
                val conversationIdList: List<Long> =
                    ConversationMembers.selectAll().where { ConversationMembers.userId eq userId }.map {
                        it[ConversationMembers.conversationId]
                    }
                conversationIdList.map { conversationId ->

                    val lastUserWhoSentAMessage = getLastMessageUsername(conversationId)

                    if (lastUserWhoSentAMessage == null) {
                        logger.warn { "getAllConversations: lastUserWhoSentAMessage is null" }
                        return@transaction null
                    }
                    val isMe = getUserIdWithinTransaction(lastUserWhoSentAMessage) == userId

                    val timeOfLastMessage = getLastMessageTimestamp(userId)

                    if (timeOfLastMessage == null) {
                        logger.warn { "getAllConversations: timeOfLastMessage is null" }
                        return@transaction null
                    }

                    val userList = getUsersInConversation(userId, conversationId)

                    if (userList == null) {
                        logger.warn { "getAllConversations: userList is null" }
                        return@transaction null
                    }


                    val publicKeys = getPgpKeysInConversation(userId, conversationId)



                    MessageConversationObject(
                        conversationId,
                        lastUserWhoSentAMessage,
                        isMe,
                        timeOfLastMessage,
                        userList,
                        publicKeys
                    )

                }
            }
        } catch (e: Exception) {
            logger.error { e.message }
            null
        }
    } else return null
}

fun getLastMessageTimestamp(group: Long): LocalDateTime? {
    return try {

        Messages.selectAll().where { (Messages.conversationId eq group) }.orderBy(
            Messages.id,
            SortOrder.DESC
        ).map { it[timeSent] }.first()

    } catch (e: Exception) {
        logger.error { e.message + "Error occurred trying to fetch the last message time for a user" }
        null
    }
}

