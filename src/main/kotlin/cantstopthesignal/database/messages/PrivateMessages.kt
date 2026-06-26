package cantstopthesignal.database.messages

import cantstopthesignal.database.notifications.numUnreadMessagesInConversation
import cantstopthesignal.database.users.getPublicKey
import cantstopthesignal.database.users.getUserId
import cantstopthesignal.database.users.getUserName
import cantstopthesignal.enums.Length
import cantstopthesignal.enums.RetValues
import cantstopthesignal.log.logger
import cantstopthesignal.table_definitions.*
import cantstopthesignal.table_definitions.Messages.message
import cantstopthesignal.table_definitions.Messages.senderId
import cantstopthesignal.table_definitions.Messages.timeSent
import org.jetbrains.exposed.v1.core.*
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
    val numUnreadMessages: Long,
    val transientMessages: Boolean,
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
                    numUnreadMessages = numUnreadMessagesInConversation(userId, conversationId),
                    it[Conversations.transientMessages],
                    totalPages = totalPages
                )
            }

            /*
                I don't see any point in keeping these so I'll just remove them once a convo is read
                I will remove the read field I dont care to keep track of read value, the lack of notif presence can be used to mark read anyway.
             */
            if (MessageNotifications.selectAll()
                    .where { (MessageNotifications.conversationId eq conversationId) and (MessageNotifications.userId eq userId) }
                    .count() > 0
            ) {
                MessageNotifications.deleteWhere {
                    (MessageNotifications.conversationId eq conversationId) and (MessageNotifications.userId eq userId)
                }
            }

            convo.firstOrNull()
        }
    } catch (e: Exception) {
        logger.error { "${e.message} Occurred while trying to fetch a single conversation." }
        null
    }
}

fun createConversation(userId: Long, users: List<Long>, conversationName: String?, selfDelete: Boolean): Long? = try {
    transaction {
        val targetUserIds = (users + userId).distinct()
        val numUsers = targetUserIds.size

        val visitedIds = mutableSetOf<Pair<Long, Long>>()
        //This can iterate a maximum of 225 times so we'll have a lil mappy poo to make sure we dont add unnecessary compute
        for (id in targetUserIds) {

            for (userId in targetUserIds) {
                if (id == userId) {
                    continue
                }
                if (visitedIds.contains(Pair(id, userId)) || visitedIds.contains(Pair(userId, id))) {
                    continue
                }

                if (isUserBlocked(userId, id)) {
                    return@transaction RetValues.BLOCKED_USER.value
                }
                visitedIds.add(Pair(id, userId))
            }
        }

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
            it[transientMessages] = selfDelete
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
                return@transaction emptyList()
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
            .firstOrNull()?.get(senderId)

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

fun getTransientMessages(conversationId: Long): Boolean {
    return try {
        transaction {
            Conversations.selectAll().where { Conversations.id eq conversationId }
                .singleOrNull()?.get(Conversations.transientMessages) == true
        }
    } catch (e: Exception) {
        logger.error { "An errror occurred while trying to get transient messages : ${e.message}" }
        false // I think it is better to falsely say there ISNT disappearing messages than say there is
    }
}

fun getAllConversations(userId: Long, page: Int, limit: Int): List<MessageConversationObject>? {
    val offsetVal = ((page - 1) * limit).toLong()
    val receiverUserNameString = getUserName(userId)
    return if (receiverUserNameString != null) {
        try {
            transaction {
                val conversationIdList: List<Long> =
                    ConversationMembers
                        .join(Messages, JoinType.LEFT, ConversationMembers.conversationId, Messages.conversationId)
                        .select(ConversationMembers.conversationId)
                        .where { ConversationMembers.userId eq userId }
                        .groupBy(ConversationMembers.conversationId)
                        .orderBy(Messages.id.max(), SortOrder.DESC)
                        .limit(limit)
                        .offset((((page - 1)) * limit).toLong())
                        .map { it[ConversationMembers.conversationId] }

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
                        numUnreadMessagesInConversation(userId, conversationId),
                        getTransientMessages(conversationId),
                        totalPages
                    )

                }
            }
        } catch (e: Exception) {
            logger.error { e.message }
            null
        }
    } else null
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

/*
    We want to encourage users to not take tons of space in the database with private messages since its a social site and should be useful to all users
    not just private messages. So we will allow users to clear their own messages from a conversation. You should not need THAT much history anyway for social
    site DMs.
 */
fun deleteAllMyMessagesInConversation(userId: Long, conversationId: Long): Boolean? {
    return try {
        transaction {
            Messages.deleteWhere { (Messages.conversationId eq conversationId) and (Messages.senderId eq userId) } > 0
        }
    } catch (e: Exception) {
        logger.error { "An error occurred while trying to delete all of a users messages from the database: " + e.message }
        null
    }
}

fun leaveConversation(userId: Long, conversationId: Long): Boolean? {
    return try {
        transaction {

            if (Conversations.innerJoin(ConversationMembers).selectAll()
                    .where { (Conversations.id eq conversationId) and (ConversationMembers.userId eq userId) }
                    .count() == 0L
            ) {
                //If we cannot find a conversation with this user in it by that ID then just send a failure, this should only happen if a user is fucking around
                //hand crafting requests
                return@transaction false
            }
            val success =
                ConversationMembers.deleteWhere { (ConversationMembers.userId) eq userId and (ConversationMembers.conversationId eq conversationId) } > 0

            //If there is nobody left, just delete all of it.
            if (ConversationMembers.selectAll().where(ConversationMembers.conversationId eq conversationId)
                    .count() <= 1
            ) {

                val memberDeleted =
                    ConversationMembers.deleteWhere { ConversationMembers.conversationId eq conversationId } > 0
                if (!memberDeleted) {
                    logger.error { "An unexpected error occurred while trying to delete a conversations members" }
                }

                val conversationDeleted = Conversations.deleteWhere { Conversations.id eq conversationId } > 0

                if (!conversationDeleted) {
                    logger.error { "An unexpected error occurred while trying to delete a conversation" }
                }

                //The database reference option constraint will delete the messages for us
            }

            success
        }
    } catch (e: Exception) {
        logger.error { "An error occurred while trying to leave conversation : " + e.message }
        null
    }
}

fun isUserBlocked(user: Long, target: Long): Boolean {
    return try {
        transaction {
            PrivateMessageBlockList.selectAll().where {
                ((PrivateMessageBlockList.blockedById eq user) and (PrivateMessageBlockList.blockedUser eq target)) or ((PrivateMessageBlockList.blockedUser eq user) and (PrivateMessageBlockList.blockedById eq target))
            }.count() > 0
        }
    } catch (e: Exception) {
        logger.error { "An error occurred while trying to determine is a user was blocked. The error message contents is ${e.message}" }
        false
    }

}

fun getBlockList(user: Long): List<String>? {
    return try {
        transaction {
/*
    We will silently limit it but not expose that to the user theyll just see most recent limit, im not going to make it accomodate looking through 100s of blocked users because that is ridiculous.
   this feature is meant to be for harassment only. Just show most recent 50 blocked users, if there are more than 50, you better remember who you blocked if you ever want to unblock them.
 */
            val list = PrivateMessageBlockList.selectAll().where(PrivateMessageBlockList.blockedById eq user).limit(
                Length.MAX_BLOCKED_USERS.value.toInt()
            ).orderBy(PrivateMessageBlockList.id, SortOrder.DESC).map {
                getUserName(it[PrivateMessageBlockList.blockedUser])!! //Because this is an FK it cant return a null user
            }


            return@transaction list
        }
    } catch (e: Exception) {
        logger.error { "An error occurred while trying to block user from messaging" + e.message }
        null
    }
}

fun blockUserFromMessaging(caller: Long, target: Long, removeFromExistingConversations: Boolean): Boolean {
    return try {
        transaction {
            val exists = PrivateMessageBlockList.selectAll()
                .where { (PrivateMessageBlockList.blockedById eq caller and (PrivateMessageBlockList.blockedUser eq target)) or ((PrivateMessageBlockList.blockedById eq target) and (PrivateMessageBlockList.blockedUser eq caller)) }
                .count() > 0
            if (exists) {
                return@transaction true
            }

            val success = PrivateMessageBlockList.insert {
                it[blockedUser] = target
                it[blockedById] = caller
            }[PrivateMessageBlockList.id]

            //I decided to make this obligatory so we will probably end up removing this branch
            if (removeFromExistingConversations) {
                val callerConversations = ConversationMembers
                    .selectAll().where { ConversationMembers.userId eq caller }
                    .map { it[ConversationMembers.conversationId] }
                    .toSet()

                val targetConversations = ConversationMembers
                    .selectAll().where { ConversationMembers.userId eq target }
                    .map { it[ConversationMembers.conversationId] }
                    .toSet()

                val conversationsToLeave = callerConversations intersect targetConversations


                for (id in conversationsToLeave) {
                    logger.debug { id }
                    val ret = leaveConversation(caller, id)
                    logger.debug { ret }
                }

            }

            return@transaction success > 0
        }
    } catch (e: Exception) {
        logger.error { "An error occurred while trying to block user from messaging" + e.message }
        false
    }

}

fun unblockUserFromMessaging(caller: Long, target: Long): Boolean {
    return try {
        transaction {
            val exists = PrivateMessageBlockList.selectAll()
                .where { (PrivateMessageBlockList.blockedById eq caller and (PrivateMessageBlockList.blockedUser eq target)) or ((PrivateMessageBlockList.blockedById eq target) and (PrivateMessageBlockList.blockedUser eq caller)) }
                .count() > 0
            if (!exists) {
                return@transaction true
            }

            //The order match is very important here otherwise you could unblock yourself when another user blocked you
            val success =
                PrivateMessageBlockList.deleteWhere { (PrivateMessageBlockList.blockedUser eq target) and (PrivateMessageBlockList.blockedById eq caller) }

            return@transaction success > 0
        }
    } catch (e: Exception) {
        logger.error { "An error occurred while trying to block user from messaging" + e.message }
        false
    }

}