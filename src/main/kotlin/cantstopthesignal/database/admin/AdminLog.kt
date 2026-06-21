package cantstopthesignal.database.admin

import cantstopthesignal.database.users.getUserName
import cantstopthesignal.database.users.isUserAdmin
import cantstopthesignal.log.logger
import cantstopthesignal.table_definitions.AdminLogs
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime
import java.time.ZoneOffset


data class AdminLog(
    val id: Long,
    val timestamp: LocalDateTime,
    val doneById: Long,
    val eventString: String,
    val reason: String
)

data class AdminLogEntry(
    val id: Long,
    val timestamp: LocalDateTime,
    val adminName: String,
    val actionString: String,
    val reason: String
)

fun insertAdminLogEntry(user: Long, reasonString: String, action: String): Boolean {

    if (!isUserAdmin(user)) {
        return false
    }

    return try {
        transaction {
            AdminLogs.insert {
                it[timestamp] = LocalDateTime.now(ZoneOffset.UTC)
                it[doneById] = user
                it[reason] = reasonString
                it[actionString] = action
            }.insertedCount > 0
        }

    } catch (e: Exception) {
        logger.error { e.message }
        false
    }

}

fun getAdminLogEntries(page: Int, limit: Int, userId: Long, order: String?): List<AdminLogEntry>? {

    if (!isUserAdmin(userId)) {
        return null
    }

    val orderBy: SortOrder = if (order == "oldest") SortOrder.ASC else SortOrder.DESC
    val offsetVal = ((page - 1) * limit).toLong()

    return try {
        transaction {
            AdminLogs.selectAll().orderBy(AdminLogs.id to orderBy)
                .limit(limit).offset(offsetVal).map {
                    AdminLogEntry(
                        it[AdminLogs.id],
                        it[AdminLogs.timestamp],
                        getUserName(it[AdminLogs.doneById]) ?: return@transaction null,
                        it[AdminLogs.actionString],
                        it[AdminLogs.reason]
                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

