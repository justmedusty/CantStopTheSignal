package cantstopthesignal.database.messages

import cantstopthesignal.cryptography.encryptMessage
import cantstopthesignal.database.users.ProfileDataEntry
import cantstopthesignal.database.users.getProfileDataEntry
import cantstopthesignal.database.users.getPublicKey
import cantstopthesignal.database.users.getUserName
import cantstopthesignal.database.users.hasAutoEncryptionEnabled
import cantstopthesignal.log.logger
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
data class Messages(
    val id: Long,
    val sender: String,
    val message: String,
    val timeSent: LocalDateTime,
)

fun sendMessage(sender: Long, receiver: Long, messageString: String): Long? {

    val publicKey: String?
    var encryptedMessage: ByteArray? = null

    if (hasAutoEncryptionEnabled(receiver)) {
        publicKey = getPublicKey(receiver)
        if (publicKey != null) {
            encryptedMessage = encryptMessage(publicKey, messageString)
        } else {
            encryptedMessage = null
        }

    }

    return try {

        transaction {

            Messages.insert {
                it[senderId] = sender
                it[receiverId] = receiver

                if (encryptedMessage != null) {
                    //TODO remember this is here and make sure this conversion works properly
                    it[message] = encryptedMessage.contentToString()
                } else {
                    it[message] = messageString
                }

                it[timeSent] = LocalDateTime.now(ZoneOffset.UTC)
            } get Messages.id


        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }

}

fun getMessagesFromUser(requesterId: Long, requestedId: Long, page: Int, limit: Int): List<Message>? {

    val offsetVal = ((page - 1) * limit).toLong()


    return try {

        transaction {
            Messages.selectAll().where { (receiverId eq requesterId) and (senderId eq requestedId) }
                .limit(limit).offset(offsetVal).map {
                    Message(
                        id = it[Messages.id],
                        senderId = requestedId,
                        receiverId = requesterId,
                        message = it[message],
                        timeSent = it[timeSent]
                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        return null //return null on error , not empty list, because empty list can mean more than just error, null can only mean error
    }
}


fun getAllMessages(userId: Long, page: Int, limit: Int): List<Message>? {
    val offsetVal = ((page - 1) * limit).toLong()
    val receiverUserNameString = getUserName(userId)
    return if (receiverUserNameString != null) {
        try {
            transaction {

                Messages.selectAll().where { (receiverId eq userId) }.limit(limit).offset(offsetVal).map {

                    Message(
                        id = it[Messages.id],
                        senderId = it[senderId],
                        receiverId = it[receiverId],
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

fun getUsersWhoHaveMessagedYou(userId: Long, page: Int, limit: Int): List<ProfileDataEntry>? {
    val offsetVal = ((page - 1) * limit).toLong()
    val usersWithProfileData = mutableMapOf<Long, ProfileDataEntry>()

    try {
        transaction {
            val senderIdsByMostRecent = Messages.selectAll().where { receiverId eq userId }
                .orderBy(Messages.id, SortOrder.DESC)
                .limit(limit).offset(offsetVal)
                .map { it[senderId] }


            senderIdsByMostRecent.forEach { senderId ->

                if (!usersWithProfileData.containsKey(senderId)) {
                    val senderProfileData = getProfileDataEntry(senderId)
                    if (senderProfileData != null) {
                        usersWithProfileData[senderId] = senderProfileData
                    }
                }
            }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        return null
    }

    return usersWithProfileData.values.toList()
}