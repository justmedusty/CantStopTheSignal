package cantstopthesignal.database.comments

import cantstopthesignal.log.logger
import cantstopthesignal.table_definitions.CommentEdits
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime
import java.time.ZoneOffset

fun insertNewCommentEdit(commentsId: Long, userId: Long): Boolean {
    return try {
        val transaction = transaction {
            CommentEdits.insert {
                it[commentId] = commentsId
                it[posterId] = userId
                it[lastEdited] = LocalDateTime.now(ZoneOffset.UTC)
            }
            true
        }
        transaction
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun getLastCommentEdit(commentId: Long): LocalDateTime? {
    return try {
        transaction {
            val result = CommentEdits.select(CommentEdits.commentId eq commentId)
                .orderBy(CommentEdits.lastEdited, SortOrder.DESC)
                .limit(1)
                .firstOrNull()

            result?.get(CommentEdits.lastEdited)
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}
