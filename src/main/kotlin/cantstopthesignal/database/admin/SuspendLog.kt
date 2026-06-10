package cantstopthesignal.database.admin

import cantstopthesignal.database.users.getUserName
import cantstopthesignal.database.users.isUserAdmin
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.SuspendLog
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime
import java.time.ZoneOffset


data class SuspendLogEntry(
    val id: Long,
    val time: LocalDateTime,
    val adminId: Long,
    val suspendedUser: Long,
    val reason: String
)

data class SuspendLog(
    val id: Long,
    val time: LocalDateTime,
    val adminUsername: String,
    val suspendedUsername: String,
    val reason: String
)


fun insertSuspendEntry(userId: Long, reasonString: String, suspendedUser: Long): Boolean {
    return try {
        transaction {
            SuspendLog.insert {
                it[timestamp] = LocalDateTime.now(ZoneOffset.UTC)
                it[adminId] = userId
                it[suspendedUserId] = suspendedUser
                it[reason] = reasonString
            }.insertedCount > 0

        }
    } catch (e: Exception) {
        logger.error { e.message }
        false

    }
}

fun getSuspendLogEntries(
    page: Int,
    limit: Int,
    userId: Long,
    order: String?
): List<cantstopthesignal.database.admin.SuspendLog>? {

    if (!isUserAdmin(userId)) {
        return null
    }

    val orderBy: SortOrder = if (order == "oldest") SortOrder.ASC else SortOrder.DESC
    val offsetVal = ((page - 1) * limit).toLong()

    return try {
        transaction {
            SuspendLog.select(
                SuspendLog.id, SuspendLog.timestamp, SuspendLog.adminId, SuspendLog.reason
            ).orderBy(SuspendLog.id to orderBy).limit(limit).offset(offsetVal).map {
                SuspendLog(
                    it[SuspendLog.id],
                    it[SuspendLog.timestamp],
                    getUserName(it[SuspendLog.adminId]) ?: return@transaction null,
                    getUserName(it[SuspendLog.suspendedUserId]) ?: return@transaction null,
                    it[SuspendLog.reason]
                )
            }
        }
    } catch (e: Exception) {
        logger.error { "${e.message} ocurred while trying to get suspend logs entries" }
        null
    }
}

