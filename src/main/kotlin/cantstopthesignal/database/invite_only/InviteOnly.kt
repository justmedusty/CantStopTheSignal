package cantstopthesignal.database.invite_only

import cantstopthesignal.database.users.isUserAdmin
import cantstopthesignal.database.users.isUserAdminOrModerator
import cantstopthesignal.log.logger
import cantstopthesignal.table_definitions.InviteCodes
import cantstopthesignal.enums.Length
import cantstopthesignal.enums.RetStrings
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

/*
    We're going to paginate it, but I do not believe it is needed. I do not think if you are running an invite only forum that you should have
    100s to 1000s of invite codes ready to go it makes sense to have a few dozen max at a time.
 */
fun getAllValidLoginCodes(userId: Long, page: Long, limit: Long): List<String>? {
    return try {
        transaction {
            if (!isUserAdminOrModerator(userId)) return@transaction null

            val offsetVal = ((page - 1) * limit)


            val codeList = InviteCodes.selectAll().offset(offsetVal).limit(limit.toInt()).map {
                it[InviteCodes.inviteCode]
            }

            codeList

        }
    } catch (e: Exception) {
        logger.error { e.message + " Occurred while trying to get all valid login codes." }
        null
    }
}

fun generateNewInviteCode(userId: Long): String? {
    return try {
        transaction {
            //Only admins can generate, mods can view invite codes
            if (!isUserAdmin(userId)) {
                return@transaction null
            }

            if(InviteCodes.selectAll().count() >= Length.MAX_INVITE_CODES.value) {
                return@transaction RetStrings.MAX_REACHED.value
            }

            val newCode = InviteCodes.insert {
                it[inviteCode] = UUID.randomUUID().toString()
            }[InviteCodes.inviteCode]

            newCode
        }
    } catch (e: Exception) {
        logger.error { e.message + " occurred while trying to generate a new invite code" }
        null
    }
}


fun isValidInviteCode(inviteCode: String): Boolean {
    return try {
        transaction {
            InviteCodes.selectAll().where { InviteCodes.inviteCode eq inviteCode }.count() > 0
        }
    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to check the validity of an invite code" }
        false
    }
}

fun consumeInviteCode(inviteCode: String): Boolean {
    return try {
        transaction {
            InviteCodes.deleteWhere { InviteCodes.inviteCode eq inviteCode } == 1
        }
    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to check the validity of an invite code" }
        false
    }
}

