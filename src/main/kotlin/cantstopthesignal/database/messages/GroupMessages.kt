package cantstopthesignal.database.messages

import cantstopthesignal.cryptography.encryptMessage
import cantstopthesignal.database.users.getPublicKey
import cantstopthesignal.database.users.getUserNameWithinTransaction
import cantstopthesignal.database.users.hasAutoEncryptionEnabled
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.GroupMemberships
import com.freedom.cantstopthesignal.database.dsl.table_definitions.GroupMessages
import com.freedom.cantstopthesignal.database.dsl.table_definitions.GroupMessages.groupId
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Groups
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages.message
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages.senderId
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Messages.timeSent
import com.freedom.cantstopthesignal.enums.Length
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime
import java.time.ZoneOffset


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


fun createGroup(groupName: String, createdById: Long, members: List<Long>, forcedEncryption: Boolean): Boolean {
    //For now this is just a boolean so this should ideally be handled further up in the routing, but I will do a check here anyway
    if (groupName.isEmpty() || groupName.length > Length.MAX_GROUPNAME_LENGTH.value) {
        return false
    }

    if (members.isEmpty()) {
        return false
    }

    return try {
        transaction {
            val newGroupId = Groups.insert {
                it[name] = name
                it[createdBy] = createdById
                it[createdAt] = LocalDateTime.now(ZoneOffset.UTC)
                it[forceEncryption] = forcedEncryption
            }[Groups.id]

            for (member in members) {
                /*Ensure user actually exists, if they pass bad values an exception will be thrown, this should never happen but we will check and skip if a value is bad.
                 * Worst case is the user ends up in a group alone because somehow all the members passed were fake.
                */
                if (getUserNameWithinTransaction(member) != null) {
                    GroupMemberships.insert {
                        it[groupId] = newGroupId
                        it[memberId] = member
                    }
                }
            }
        }
        true
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }

}

fun addMemberToGroup(groupId: Long, newMember: Long): Boolean {
    return try {
        transaction {

            if (getUserNameWithinTransaction(newMember) != null) {
                GroupMemberships.insert {
                    it[GroupMemberships.groupId] = groupId
                    it[memberId] = newMember
                }
            } else {
                //If the user isn't valid return false, we could also rely on the SQL exception thrown but I am doing this for now
                return@transaction false
            }


            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}


fun removeMemberFromGroup(groupId: Long, member: Long): Boolean {
    return try {
        transaction {

            if (getUserNameWithinTransaction(member) != null) {
                GroupMemberships.deleteWhere {
                    GroupMemberships.groupId eq groupId and GroupMemberships.memberId.eq(
                        member
                    )
                }
            } else {
                //If the user isn't valid return false, we could also rely on the SQL exception thrown but I am doing this for now
                return@transaction false
            }

            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}