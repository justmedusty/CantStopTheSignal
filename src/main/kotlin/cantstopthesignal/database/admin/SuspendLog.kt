package cantstopthesignal.database.admin

import cantstopthesignal.database.users.isUserAdmin
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.SuspendLog
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
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
    val suspend: Boolean,
    val reason: String
)


fun insertSuspendEntry(userId: Long, isSuspend: Boolean, reasonString: String, suspendedUser: Long): Boolean {
    return try {
        transaction {
            SuspendLog.insert {
                it[timestamp] = LocalDateTime.now(ZoneOffset.UTC)
                it[adminId] = userId
                it[suspendedUserId] = suspendedUser
                it[suspend] = isSuspend
                it[reason] = reasonString
            }.insertedCount > 0

        }
    } catch (e: Exception) {
        logger.error { e.message }
        false

    }
}

fun getSuspendLogEntries(page: Int, limit: Int, userId: Long, order: String?): List<SuspendLogEntry>? {

    if (!isUserAdmin(userId)) {
        return null
    }

    val orderBy: SortOrder = if (order == "oldest") SortOrder.ASC else SortOrder.DESC
    val offsetVal = ((page - 1) * limit).toLong()

    return try {
        transaction {
            SuspendLog.select(
                SuspendLog.id, SuspendLog.timestamp, SuspendLog.adminId, SuspendLog.suspend, SuspendLog.reason
            ).orderBy(SuspendLog.id to orderBy).limit(limit).offset(offsetVal).map {
                SuspendLogEntry(
                    it[SuspendLog.id],
                    it[SuspendLog.timestamp],
                    it[SuspendLog.adminId],
                    it[SuspendLog.suspendedUserId],
                    it[SuspendLog.suspend],
                    it[SuspendLog.reason]
                )
            }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

fun getSuspendLogEntriesByAdmin(
    page: Int,
    limit: Int,
    userId: Long,
    order: String?,
    adminId: Long
): List<SuspendLogEntry>? {

    if (!isUserAdmin(userId)) {
        return null
    }


    val orderBy: SortOrder = if (order == "oldest") SortOrder.ASC else SortOrder.DESC
    val offsetVal = ((page - 1) * limit).toLong()

    return try {
        transaction {
            SuspendLog.select(
                SuspendLog.id, SuspendLog.timestamp, SuspendLog.adminId, SuspendLog.suspend, SuspendLog.reason
            ).where { SuspendLog.adminId eq adminId }.orderBy(SuspendLog.timestamp to orderBy).limit(limit)
                .offset(offsetVal)
                .map {
                    SuspendLogEntry(
                        it[SuspendLog.id],
                        it[SuspendLog.timestamp],
                        it[SuspendLog.adminId],
                        it[SuspendLog.suspendedUserId],
                        it[SuspendLog.suspend],
                        it[SuspendLog.reason]
                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}