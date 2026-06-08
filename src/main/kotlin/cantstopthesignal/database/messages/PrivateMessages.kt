package cantstopthesignal.database.messages

import cantstopthesignal.database.users.getPublicKey
import cantstopthesignal.database.users.getUserId
import cantstopthesignal.database.users.getUserName
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.ConversationMembers
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Conversations
import com.freedom.cantstopthesignal.database.dsl.table_definitions.MessageNotifications
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages.message
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages.senderId
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages.timeSent
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.RetValues
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.ceil


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
    val lastSender: String?,
    val isMe: Boolean?,
    val timeOfLastMessage: LocalDateTime?,
    val name: String?,
    val members: List<String>,
    val pgpKey: List<String>?, //This is just here to prompt users to encrypt their own messages with the convo members uploaded IDs
    val totalPages: Long
)


fun sendMessage(sender: Long, conversation: Long, messageString: String): Long? {

    return try {

        transaction {

            val id = Messages.insert {
                it[senderId] = sender
                it[conversationId] = conversation
                it[message] = messageString
                it[timeSent] = LocalDateTime.now(ZoneOffset.UTC)
            }.get(Messages.id)

            val otherUsersInChat =
                ConversationMembers.selectAll().where { ConversationMembers.conversationId eq conversation }.filter {
                    it[ConversationMembers.userId] != sender
                }.map {
                    it[ConversationMembers.userId]
                }

            for (otherUser in otherUsersInChat) {
                MessageNotifications.insert {
                    it[MessageNotifications.userId] = otherUser
                    it[MessageNotifications.conversationId] = conversation
                }
            }

            return@transaction id
        }

    } catch (e: Exception) {
        logger.error { e.message }
        null
    }

}

fun getConversation(userId: Long, conversationId: Long): MessageConversationObject? {
    return try {
        transaction {
            val totalPages = ceil(
                Messages.selectAll().where { Messages.conversationId eq conversationId }.count()
                    .toDouble() / Length.MAX_CONVERSATION_MESSAGE_LIMIT.value.toDouble()
            ).toLong()

            val convo = Conversations.selectAll().where { Conversations.id eq conversationId }.map {
                MessageConversationObject(
                    id = it[Conversations.id],
                    lastSender = null, //We shouldn't need these 3 from inside a message conversation so this should be fine to skip,
                    isMe = null,
                    timeOfLastMessage = null,
                    name = it[Conversations.name],
                    members = getMembersOfConversation(conversationId)!! /* This should already be sanitized so we will assert it not null*/,
                    pgpKey = getPgpKeysInConversation(userId, conversationId),
                    totalPages = totalPages
                )
            }

            /*
                I don't see any point in keeping these so I'll just remove them once a convo is read
                I will remove the read field I dont care to keep track of read value, the lack of notif presence can be used to mark read anyway.
             */
            if(MessageNotifications.selectAll().where{ (MessageNotifications.conversationId eq conversationId) and (MessageNotifications.userId eq userId) }.count() > 0){
                MessageNotifications.deleteWhere {
                    (MessageNotifications.conversationId eq conversationId) and (MessageNotifications.userId eq userId)
                }
            }

            convo.firstOrNull()
        }
    } catch (e: Exception) {
        logger.error { " Occurred while trying to fetch a single conversation." }
        null
    }
}

fun createConversation(userId: Long, users: List<Long>, conversationName: String?): Long? = try {
    transaction {
        val targetUserIds = (users + userId).distinct()
        val numUsers = targetUserIds.size

        val matchingConversationId = targetUserIds
            .map { uid ->
                ConversationMembers
                    .select(ConversationMembers.conversationId)
                    .where { ConversationMembers.userId eq uid }
                    .map { it[ConversationMembers.conversationId] }
                    .toSet()
            }
            .reduce { acc, ids -> acc intersect ids }
            .firstOrNull { candidateId ->
                // Ensure the conversation has exactly numUsers members (no extras)
                ConversationMembers
                    .selectAll()
                    .where { ConversationMembers.conversationId eq candidateId }
                    .count() == numUsers.toLong()
            }

        if (matchingConversationId != null) {
            return@transaction RetValues.ALREADY_EXISTS.value
        }

        val conversationId = Conversations.insert {
            it[createdBy] = userId
            it[isGroup] = targetUserIds.size > 2
            it[name] =
                conversationName // This is null for non groups and for groups where it is null it will just be populated with the usernames
        }[Conversations.id]


        targetUserIds.forEach { userId ->
            ConversationMembers.insert {
                it[joinedAt] = LocalDateTime.now(ZoneOffset.UTC)
                it[ConversationMembers.conversationId] = conversationId
                it[ConversationMembers.userId] = userId
            }
        }


        conversationId

    }
} catch (e: Exception) {
    logger.error { e.message + " occurred during createConversation call" }
    null
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
            /* Make sure the person requesting this is actually part of the conversation */
            val validityCheck = ConversationMembers.selectAll()
                .where { (ConversationMembers.conversationId eq conversationId) and (ConversationMembers.userId eq requesterId) }
                .singleOrNull()

            if (validityCheck == null) {
                return@transaction null
            }

            val hasMessages = Messages.selectAll().where { Messages.conversationId eq conversationId }.count() > 0

            if (!hasMessages) {
                return@transaction emptyList<MessageObject>()
            }

            val messageList = Messages
                .selectAll()
                .where { Messages.conversationId eq conversationId }
                .limit(limit)
                .offset(offsetVal)
                .orderBy(timeSent to SortOrder.DESC)
                .map {
                    MessageObject(
                        id = it[Messages.id],
                        sender = getUserName(it[senderId])!!, // This should not conceivably be null so we will force this for now
                        message = it[message],
                        timeSent = it[timeSent],
                        isMe = it[senderId] == requesterId
                    )
                }

            if (messageList.isEmpty()) {
                return@transaction emptyList()
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
                getUserName(it[ConversationMembers.userId])!! //There should not be any null entries here we will verify that the conversation actually exists somewhere
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
            .firstOrNull()?.get(Messages.conversationId)

        if (userId == null) {
            return null
        }

        getUserName(userId)

    } catch (e: Exception) {
        logger.error { e.message + " occurred while trying to get last username " }
        null
    }
}

fun getUsersInConversation(userId: Long, groupId: Long): List<String>? {

    return try {
        val list = ConversationMembers.selectAll()
            .where { (ConversationMembers.conversationId eq groupId) and (ConversationMembers.userId neq userId) }.map {
                getUserName(it[ConversationMembers.userId])
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

            } ?: null
        val keyList: List<String> = list?.filterNotNull()?.map { it } ?: emptyList()
        keyList
    } catch (e: Exception) {
        logger.error { e.message + " happened while trying to fetch users pgp keys in conversation" }
        null
    }

}

//Make sure convo exists and if it does make sure that the user is part of it
fun verifyConversationId(id: Long, userId: Long): Boolean? {
    return try {
        transaction {
            val id =
                Conversations.selectAll().where { Conversations.id eq id }.limit(1).firstOrNull()?.get(Conversations.id)

            if (id == null) {
                return@transaction false
            }

            val isUserPartOfCOnversation = ConversationMembers.selectAll()
                .where { (ConversationMembers.conversationId eq id) and (ConversationMembers.userId eq userId) }
                .firstOrNull()

            if (isUserPartOfCOnversation == null) {
                return@transaction false
            }

            true

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
                    ConversationMembers.selectAll().where { ConversationMembers.userId eq userId }.limit(limit)
                        .offset((((page - 1)) * limit).toLong()).map {
                            it[ConversationMembers.conversationId]
                        }

                val totalConversations =
                    ConversationMembers.selectAll().where { ConversationMembers.userId eq userId }.count()

                val totalPages = ceil(totalConversations.toDouble() / limit.toDouble()).toLong()
                conversationIdList.map { conversationId ->

                    val lastUserWhoSentAMessage = getLastMessageUsername(conversationId)

                    val isMe =
                        if (lastUserWhoSentAMessage != null) getUserId(lastUserWhoSentAMessage) == userId else null

                    val timeOfLastMessage = getLastMessageTimestamp(conversationId)
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
                        getConversationName(conversationId),
                        userList,
                        publicKeys,
                        totalPages
                    )

                }
            }
        } catch (e: Exception) {
            logger.error { e.message }
            null
        }
    } else return null
}

fun getConversationName(conversationId: Long): String? {
    return try {
        Conversations.selectAll().where { Conversations.id eq conversationId }.first()[Conversations.name]
    } catch (e: Exception) {
        logger.error { e.message + "Happened while trying to fetch a conversation name" }
        null
    }
}

fun getLastMessageTimestamp(group: Long): LocalDateTime? {
    return try {

        Messages.selectAll().where { (Messages.conversationId eq group) }.orderBy(
            Messages.id,
            SortOrder.DESC
        ).map { it[timeSent] }.firstOrNull()

    } catch (e: Exception) {
        logger.error { e.message + "Error occurred trying to fetch the last message time for a user" }
        null
    }
}

