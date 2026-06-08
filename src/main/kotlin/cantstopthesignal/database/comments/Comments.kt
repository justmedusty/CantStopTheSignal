package cantstopthesignal.database.comments

import cantstopthesignal.database.notifications.insertNotification
import cantstopthesignal.database.users.getUserName
import cantstopthesignal.database.users.isUserAdmin
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.CommentDislikes
import com.freedom.cantstopthesignal.database.dsl.table_definitions.CommentLikes
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Comments
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Comments.parentCommentId
import com.freedom.cantstopthesignal.database.posts.getPostOwnerId
import com.freedom.cantstopthesignal.enums.Notif
import com.freedom.cantstopthesignal.enums.RetValues
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.ceil

data class Comment(
    val id: Long,
    val content: String,
    val postId: Long,
    val commenterId: Long,
    val commenterUsername: String,
    val isReply: Boolean,
    val parentCommentId: Long?,
    val timeStamp: LocalDateTime,
    val commentLikes: Long,
    val commentDislikes: Long,
    val lastEdited: String?,
    val isCommentLikedByMe: Boolean,
    val isCommentDislikedByMe: Boolean,
    val hasReplies: Boolean,
    val myComment: Boolean,
    val totalPages: Long //This will be duplicated more than is needed but this makes it easier for me to insert it right in depending on the query being made
)

/*
    We need to do duplicate checks because if you click the browser refresh page after posting a comment or post it will send the request again
    so this can happen pretty easily. Need to check.
 */
fun isDuplicateComment(
    content: String,
    commenterId: Long,
    postId: Long,
    parentCommentId: Long?,
    isReply: Boolean
): Boolean {
    return try {
        transaction {


            if (Comments.selectAll()
                    .where { (Comments.commenterId eq commenterId) and (Comments.content eq content) and (Comments.postId eq postId) and (Comments.parentCommentId eq parentCommentId) and (Comments.isReply eq isReply) }
                    .count() != 0L
            ) {
                return@transaction true
            }
            false
        }
    } catch (e: Exception) {
        logger.error { e.message + " An error occured while trying to check if a comment is a duplicate" }
        true

    }
}

fun getUserIdFromCommentId(commentId: Long): Long? {
    return try {
        transaction {
            Comments.selectAll().where { Comments.id eq commentId }.singleOrNull()?.get(Comments.commenterId)
        }
    } catch (e: Exception) {
        logger.error { e.message + "An error occured getting a user id from a comment id" }
        null
    }
}

fun postComment(content: String, commenterId: Long, postId: Long, isReply: Boolean, parentCommentId: Long?): Long? {
    return try {
        transaction {
            if (isDuplicateComment(content, commenterId, postId, parentCommentId, false)) {
                return@transaction RetValues.ALREADY_EXISTS.value
            }
            val ret = Comments.insert {
                it[Comments.content] = content
                it[Comments.postId] = postId
                it[Comments.commenterId] = commenterId
                it[Comments.isReply] = isReply
                it[Comments.parentCommentId] = parentCommentId
                it[timeStamp] = LocalDateTime.now(ZoneOffset.UTC)
            } get Comments.id
            val postOwnerId = getPostOwnerId(postId)
            insertNotification(
                postId,
                null,
                postOwnerId!! /* We will verify this at a higher layer */,
                commenterId,
                Notif.POST_COMMENT.value
            )

            if (isReply) {
                val parentCommenterId =
                    getUserIdFromCommentId(parentCommentId!! /* We will also sanitize this higher up */)
                insertNotification(
                    postId,
                    parentCommentId,
                    parentCommenterId!! /* We will also ALSO  sanitize this higher up  */,
                    commenterId,
                    Notif.COMMENT_REPLY.value
                )
            } else {
                insertNotification(
                    postId,
                    ret,
                    postOwnerId,
                    commenterId,
                    Notif.POST_COMMENT.value
                )
            }
            ret
        }
    } catch (e: Exception) {
        e.printStackTrace()
        logger.error { e.message }
        null
    }
}

fun getParentId(commentId: Long): Long? {
    return try {
        transaction {
            val comment = Comments.select(Comments.id eq commentId).singleOrNull() ?: return@transaction null
            comment[parentCommentId]!!
        }

    } catch (ex: Exception) {
        null
    }
}

fun doesCommentHaveReplies(commentId: Long): Boolean {
    return try {
        val count =
            Comments.selectAll().where { (parentCommentId eq commentId) and (parentCommentId.isNotNull()) }.count()
        return count > 0

    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun getCommentOwnerId(commentId: Long): Long? {
    return try {

        val result = Comments.selectAll().where(Comments.id eq commentId).singleOrNull()
        result?.get(Comments.commenterId)

    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

fun isIdCommentPoster(userId: Long, commentId: Long): Boolean {
    return try {
        val match = Comments.select((Comments.id eq commentId) and (Comments.commenterId eq userId))
        match.count() > 0
    } catch (e: Exception) {
        logger.error { "Error checking who is comment poster" }
        false
    }
}

fun deleteCommentById(commentId: Long, requesterId: Long): Boolean {
    return if (isUserAdmin(requesterId) || isIdCommentPoster(requesterId, commentId)) {
        try {
            transaction {
                val success = Comments.deleteWhere { id eq commentId }
                success > 0 // Check if any rows were deleted
            }
        } catch (e: Exception) {
            logger.error { e.message }
            false
        }
    } else false

}


fun updateComment(userId: Long, commentId: Long, newComment: String): Boolean {
    return if (isIdCommentPoster(userId, commentId)) {
        try {
            transaction {
                Comments.update({ Comments.id eq commentId }) {
                    it[content] = newComment
                }
            }
            insertNewCommentEdit(commentId, userId)
        } catch (e: Exception) {
            logger.error { e.message }
            false
        }
    } else {
        false
    }
}

fun getCommentById(id: Long, userId: Long?): Comment? {
    return try {
        transaction {
            val commentLikes: Long = getLikesForComment(id)
            val commentDislikes: Long = getDislikesForComment(id)
            val lastEdited: LocalDateTime? = getLastCommentEdit(id)
            val isCommentLiked: Boolean = isCommentLikedByUser(id, userId)
            val isCommentDisliked: Boolean = isCommentDisLikedByUser(id, userId)
            val hasReplies = doesCommentHaveReplies(id)


            Comments.selectAll().where { Comments.id eq id }.singleOrNull()?.let {
                val username: String = getUserName(it[Comments.commenterId]) ?: "Couldn't load"
                Comment(
                    it[Comments.id],
                    it[Comments.content],
                    it[Comments.postId],
                    it[Comments.commenterId],
                    username,
                    it[Comments.isReply],
                    it[parentCommentId],
                    it[Comments.timeStamp],
                    commentLikes,
                    commentDislikes,
                    lastEdited.toString(),
                    isCommentLiked,
                    isCommentDisliked,
                    hasReplies,
                    it[Comments.commenterId] == userId,
                    0 //no pages for a 1 comment query
                )
            }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        e.printStackTrace()
        null
    }
}

fun getCommentsByPost(postId: Long, pageSize: Int, page: Int, userId: Long?, order: String?): List<Comment>? {
    val sortOrder: SortOrder?
    val orderByColumn = Comments.id
    var orderByCount: Count? = null

    when (order) {
        "oldest" -> {
            sortOrder = SortOrder.ASC
        }

        "newest" -> {
            sortOrder = SortOrder.DESC
        }

        "likes" -> {
            orderByCount = CommentLikes.commentId.count()
            sortOrder = SortOrder.DESC
        }

        "dislikes" -> {
            orderByCount = CommentDislikes.commentId.count()
            sortOrder = SortOrder.DESC
        }

        else -> {
            sortOrder = SortOrder.DESC
        }
    }

    return try {
        transaction {
            val query = Comments.leftJoin(CommentLikes).leftJoin(CommentDislikes).select(
                Comments.id,
                Comments.content,
                Comments.postId,
                Comments.commenterId,
                Comments.isReply,
                parentCommentId,
                Comments.timeStamp
            ).where { (Comments.postId eq postId) and (Comments.isReply eq false) }
                .limit(pageSize).offset(((page - 1) * pageSize).toLong())

            if (orderByCount != null) {
                query.orderBy(orderByCount, sortOrder).groupBy(Comments.id)
            } else {
                query.orderBy(orderByColumn, sortOrder).groupBy(Comments.id)
            }
            val totalPages = ceil(Comments.selectAll().where {
                (Comments.postId eq postId) and (Comments.isReply eq Op.FALSE)
            }.count().toDouble() / pageSize.toDouble()).toLong()

            query.map {
                val commentLikes: Long = getLikesForComment(it[Comments.id])
                val commentDislikes: Long = getDislikesForComment(it[Comments.id])
                val lastEdited: LocalDateTime? = getLastCommentEdit(it[Comments.id])
                val isCommentLiked: Boolean = isCommentLikedByUser(it[Comments.id], userId)
                val isCommentDisliked: Boolean = isCommentDisLikedByUser(it[Comments.id], userId)
                val hasReplies = doesCommentHaveReplies(it[Comments.id])
                val username: String = getUserName(it[Comments.commenterId]) ?: "Couldn't load"


                Comment(
                    it[Comments.id],
                    it[Comments.content],
                    it[Comments.postId],
                    it[Comments.commenterId],
                    username,
                    it[Comments.isReply],
                    it[parentCommentId],
                    it[Comments.timeStamp],
                    commentLikes,
                    commentDislikes,
                    lastEdited.toString(),
                    isCommentLiked,
                    isCommentDisliked,
                    hasReplies,
                    it[Comments.commenterId] == userId,
                    totalPages
                )
            }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

fun getCommentsByUser(userId: Long, pageSize: Int, page: Int, requesterId: Long?): List<Comment>? {
    return try {
        transaction {
            val totalPages = Comments.selectAll().where(Comments.postId eq userId).count() / pageSize.toLong()
            Comments.select(Comments.commenterId eq userId)
                .limit(pageSize).offset(((page - 1) * pageSize).toLong()).map {
                    val commentLikes: Long = getLikesForComment(it[Comments.id])
                    val commentDislikes: Long = getDislikesForComment(it[Comments.id])
                    val lastEdited: LocalDateTime? = getLastCommentEdit(it[Comments.id])
                    val isCommentLiked: Boolean = isCommentLikedByUser(it[Comments.id], requesterId)
                    val isCommentDisliked: Boolean =
                        isCommentDisLikedByUser(it[Comments.id], requesterId)
                    val hasReplies = doesCommentHaveReplies(it[Comments.id])
                    val username: String = getUserName(it[Comments.commenterId]) ?: "Couldn't load"

                    Comment(
                        it[Comments.id],
                        it[Comments.content],
                        it[Comments.postId],
                        it[Comments.commenterId],
                        username,
                        it[Comments.isReply],
                        it[parentCommentId],
                        it[Comments.timeStamp],
                        commentLikes,
                        commentDislikes,
                        lastEdited.toString(),
                        isCommentLiked,
                        isCommentDisliked,
                        hasReplies,
                        it[Comments.commenterId] == requesterId,
                        totalPages
                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

fun getReplyComments(commentId: Long, pageSize: Int, page: Int, requesterId: Long?): List<Comment>? {
    return try {
        transaction {
            val parentComment = Comments.selectAll().where { Comments.id eq commentId }.singleOrNull()
            val hasReplies = doesCommentHaveReplies(commentId)
            val totalPages = ceil(
                Comments.selectAll().where { parentCommentId eq commentId }.count()
                .toDouble() / pageSize.toDouble()
            ).toLong()
            val parentCommentData = parentComment?.let {
                val username: String = getUserName(it[Comments.commenterId]) ?: "Couldn't load"
                Comment(
                    it[Comments.id],
                    it[Comments.content],
                    it[Comments.postId],
                    it[Comments.commenterId],
                    username,
                    it[Comments.isReply],
                    it[parentCommentId],
                    it[Comments.timeStamp],
                    getLikesForComment(it[Comments.id]),
                    getDislikesForComment(it[Comments.id]),
                    getLastCommentEdit(it[Comments.id]).toString(),
                    isCommentLikedByUser(it[Comments.id], requesterId),
                    isCommentLikedByUser(it[Comments.id], requesterId),
                    hasReplies,
                    it[Comments.commenterId] == requesterId,
                    0
                )
            }

            val childComments = Comments.selectAll().where { (parentCommentId eq commentId) }
                .limit(pageSize).offset(((page - 1) * pageSize).toLong()).map {
                    val commentLikes: Long = getLikesForComment(it[Comments.id])
                    val commentDislikes: Long = getDislikesForComment(it[Comments.id])
                    val lastEdited: LocalDateTime? = getLastCommentEdit(it[Comments.id])
                    val isCommentLiked: Boolean = isCommentLikedByUser(it[Comments.id], requesterId)
                    val isCommentDisliked: Boolean =
                        isCommentDisLikedByUser(it[Comments.id], requesterId)
                    val hasRepliesChild = doesCommentHaveReplies(it[Comments.id])
                    val username: String = getUserName(it[Comments.commenterId]) ?: "Couldn't load"
                    Comment(
                        it[Comments.id],
                        it[Comments.content],
                        it[Comments.postId],
                        it[Comments.commenterId],
                        username,
                        it[Comments.isReply],
                        it[parentCommentId],
                        it[Comments.timeStamp],
                        commentLikes,
                        commentDislikes,
                        lastEdited.toString(),
                        isCommentLiked,
                        isCommentDisliked,
                        hasRepliesChild,
                        it[Comments.commenterId] == requesterId,
                        totalPages
                    )
                }

            parentCommentData?.let { childComments } ?: childComments
        }
    } catch (e: Exception) {
        logger.error { e.message }
    } as List<Comment>?
}

fun getPostIdFromComment(commentId: Long): Long? {
    return try {
        transaction {
            Comments.selectAll().where { Comments.id eq commentId }.singleOrNull()?.get(Comments.postId)
        }
    } catch (e: Exception) {
        logger.error { "Error trying to get the post id! ${e}" }
        null
    }
}

/*
    Sanity check helper function to make sure people aren't trying to interact with posts that dont exist
 */
fun verifyCommentId(id: Long): Boolean {
    return try {
        transaction {
            Comments.select(Comments.id eq id).count() > 0
        }
    } catch (e: Exception) {
        logger.error { e }
        false
    }
}