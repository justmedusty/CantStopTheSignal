package cantstopthesignal.database.messages

import cantstopthesignal.cryptography.encryptMessage
import cantstopthesignal.database.users.getPublicKey
import cantstopthesignal.database.users.hasAutoEncryptionEnabled
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.GroupMessages
import com.freedom.cantstopthesignal.database.dsl.table_definitions.GroupMessages.groupId
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages.message
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages.senderId
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages.timeSent
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime


data class Group(
    val id: Long,
    val name: String,
    val createdBy: Long,
    val createdAt: LocalDateTime,
    val forceEncryption: Boolean,
)

data class GroupMessage(
    val id: Long,
    val senderId: Long,
    val groupId: Long,
    val message: String,
    val timeSent: LocalDateTime,
)


fun getAllMessagesForGroup(id: Long, page: Int, limit: Int): List<GroupMessage>? {
    val offsetVal = ((page - 1) * limit).toLong()
    return try {
        transaction {

            GroupMessages.selectAll().where { (groupId eq id) }.limit(limit).offset(offsetVal).map {

                GroupMessage(
                    id = it[Messages.id],
                    senderId = it[senderId],
                    groupId = it[groupId],
                    message = it[message],
                    timeSent = it[timeSent]
                )
            }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }

}
