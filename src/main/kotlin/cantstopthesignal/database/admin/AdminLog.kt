package cantstopthesignal.database.admin

import cantstopthesignal.database.users.isUserAdmin
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.AdminLogs
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime


data class AdminLog(
    val id: Long,
    val timestamp: LocalDateTime,
    val userId: Long,
    val doneById: Long,
    val added: Boolean,
    val reason: String
)


fun insertAdminLogEntry(user: Long,doneBy: Long,reasonString: String, addedBool: Boolean) : Boolean{

    if(!isUserAdmin(user)){
        return false
    }

    return try {
        transaction {
            AdminLogs.insert {
                it[timestamp] = LocalDateTime.now()
                it[userId] = user
                it[doneById] = doneBy
                it[added] = addedBool
                it[reason] = reasonString
            }.insertedCount > 0
        }

    }catch (e:Exception){
        logger.error { e.message }
        false
    }

}

fun getAdminLogEntries(page: Int, limit: Int, userId: Long, order: String?): List<AdminLog>? {

    if(!isUserAdmin(userId)){
        return null
    }

    val orderBy: SortOrder = if (order == "oldest") SortOrder.ASC else SortOrder.DESC
    val offsetVal = ((page - 1) * limit).toLong()

    return try {
        transaction {
            AdminLogs.select(
                AdminLogs.id,
                AdminLogs.timestamp,
                AdminLogs.userId,
                AdminLogs.doneById,
                AdminLogs.added,
                AdminLogs.reason
            ).orderBy(AdminLogs.id to orderBy)
                .limit(limit).offset(offsetVal).map {
                    AdminLog(
                        it[AdminLogs.id],
                        it[AdminLogs.timestamp],
                        it[AdminLogs.userId],
                        it[AdminLogs.doneById],
                        it[AdminLogs.added],
                        it[AdminLogs.reason]
                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

fun getAdminLogEntriesByAdmin(page: Int, limit: Int, userId: Long, order: String?,requestedId: Long): List<AdminLog>? {

    if(!isUserAdmin(userId)){
        return null
    }

    val orderBy: SortOrder = if (order == "oldest") SortOrder.ASC else SortOrder.DESC
    val offsetVal = ((page - 1) * limit).toLong()

    return try {
        transaction {
            AdminLogs.select(
                AdminLogs.id,
                AdminLogs.timestamp,
                AdminLogs.userId,
                AdminLogs.doneById,
                AdminLogs.added,
                AdminLogs.reason
            ).where(AdminLogs.doneById eq requestedId).orderBy(AdminLogs.id to orderBy)
                .limit(limit).offset(offsetVal).map {
                    AdminLog(
                        it[AdminLogs.id],
                        it[AdminLogs.timestamp],
                        it[AdminLogs.userId],
                        it[AdminLogs.doneById],
                        it[AdminLogs.added],
                        it[AdminLogs.reason]
                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}