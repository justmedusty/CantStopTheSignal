package cantstopthesignal.database.posts

import cantstopthesignal.database.users.isUserAdmin
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.PostEdits
import com.freedom.cantstopthesignal.database.posts.verifyUserId
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime
import java.time.ZoneOffset

fun insertNewPostEdit(post: Long, poster: Long): Boolean {
    return try {
        transaction {
            PostEdits.insert {
                it[postId] = post
                it[lastEdited] = LocalDateTime.now(ZoneOffset.UTC)
            }
            true

        }
    } catch (e: Exception) {
        logger.error { "An error occurred while trying to edit post : ${e.message}" }
        false
    }
}

fun editPost(postId: Long, userId: Long, newTitle: String?, newPostContents: String?): Boolean {
    return transaction {
        if (verifyUserId(userId, postId) || isUserAdmin(userId)) {

            if (newTitle == null && newPostContents == null) {
                return@transaction false
            }
            return@transaction try {
                if (updatePostContents(newTitle, newPostContents, postId)) {
                    insertNewPostEdit(postId, userId)
                } else {
                    false
                }

            } catch (e: Exception) {
                logger.error { "Error while trying to edit post : ${e.message}" }
                return@transaction false
            }
        } else {
            return@transaction false
        }
    }

}

fun checkLastPostEdit(postId: Long): LocalDateTime? {
    return try {
        transaction {

            val latestEdit = PostEdits.select(PostEdits.postId eq postId)
                .orderBy(PostEdits.lastEdited, SortOrder.DESC)
                .limit(1)
                .singleOrNull()

            latestEdit?.getOrNull(PostEdits.lastEdited)
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}