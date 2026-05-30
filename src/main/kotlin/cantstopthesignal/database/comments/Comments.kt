package cantstopthesignal.database.comments

import cantstopthesignal.database.users.getUserName
import cantstopthesignal.database.users.getUserNameWithinTransaction
import cantstopthesignal.database.users.isUserAdmin
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.CommentDislikes
import com.freedom.cantstopthesignal.database.dsl.table_definitions.CommentLikes
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Comments
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Comments.parentCommentId
import com.freedom.cantstopthesignal.enums.RetValues
import org.jetbrains.exposed.v1.core.Count
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.text.get

data class Comment(
    val id: Long,
    val content: String,
    val postId: Long,
    val commenterId: Long,
    val commenterUsername: String,
    val isReply: Boolean,
    val parentCommentId: Long?,
    val timeStamp: String,
    val commentLikes: Long,
    val commentDislikes: Long,
    val lastEdited: String?,
    val isCommentLikedByMe: Boolean,
    val isCommentDislikedByMe: Boolean,
    val hasReplies: Boolean,
    val myComment: Boolean
)

/*
    We need to do duplicate checks because if you click the browser refresh page after posting a comment or post it will send the request again
    so this can happen pretty easily. Need to check.
 */
fun isDuplicateComment(content: String, commenterId: Long, postId: Long, parentCommentId: Long?): Boolean {
    return try {
        if (Comments.selectAll()
                .where { (Comments.commenterId eq commenterId) and (Comments.content eq content) and (Comments.postId eq postId) and (Comments.parentCommentId eq parentCommentId) }
                .count() != 0L
        ) {
            return true
        }
        false
    } catch (e: Exception) {
        logger.error { e.message + " An error occured while trying to check if a comment is a duplicate" }
        true

    }
}

fun postComment(content: String, commenterId: Long, postId: Long, isReply: Boolean, parentCommentId: Long?): Long? {
    return try {
        transaction {
            if (isDuplicateComment(content, commenterId, postId, parentCommentId)) {
                return@transaction RetValues.ALREADY_EXISTS.value
            }
            Comments.insert {
                it[Comments.content] = content
                it[Comments.postId] = postId
                it[Comments.commenterId] = commenterId
                it[Comments.isReply] = isReply
                it[Comments.parentCommentId] = parentCommentId
                it[timeStamp] = LocalDateTime.now(ZoneOffset.UTC)
            } get Comments.id

        }
    } catch (e: Exception) {
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

        val result = Comments.select(Comments.id eq commentId).singleOrNull()
        result?.get(Comments.commenterId)

    } catch (e: Exception) {
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
            val lastEdited: LocalDateTime? = getLastCommentEditWithinTransaction(id)
            val isCommentLiked: Boolean = isCommentLikedByUserWithinTransaction(id, userId)
            val isCommentDisliked: Boolean = isCommentDisLikedByUserWithinTransaction(id, userId)
            val hasReplies = doesCommentHaveReplies(id)


            Comments.select(Comments.id eq id).singleOrNull()?.let {
                val username: String = getUserNameWithinTransaction(it[Comments.commenterId]) ?: "Couldn't load"
                Comment(
                    it[Comments.id],
                    it[Comments.content],
                    it[Comments.postId],
                    it[Comments.commenterId],
                    username,
                    it[Comments.isReply],
                    it[parentCommentId],
                    it[Comments.timeStamp].toString(),
                    commentLikes,
                    commentDislikes,
                    lastEdited.toString(),
                    isCommentLiked,
                    isCommentDisliked,
                    hasReplies,
                    it[Comments.commenterId] == userId
                )
            }
        }
    } catch (e: Exception) {
        logger.error { e.message }
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

            query.map {
                val commentLikes: Long = getLikesForComment(it[Comments.id])
                val commentDislikes: Long = getDislikesForComment(it[Comments.id])
                val lastEdited: LocalDateTime? = getLastCommentEditWithinTransaction(it[Comments.id])
                val isCommentLiked: Boolean = isCommentLikedByUserWithinTransaction(it[Comments.id], userId)
                val isCommentDisliked: Boolean = isCommentDisLikedByUserWithinTransaction(it[Comments.id], userId)
                val hasReplies = doesCommentHaveReplies(it[Comments.id])
                val username: String = getUserNameWithinTransaction(it[Comments.commenterId]) ?: "Couldn't load"

                Comment(
                    it[Comments.id],
                    it[Comments.content],
                    it[Comments.postId],
                    it[Comments.commenterId],
                    username,
                    it[Comments.isReply],
                    it[parentCommentId],
                    it[Comments.timeStamp].toString(),
                    commentLikes,
                    commentDislikes,
                    lastEdited.toString(),
                    isCommentLiked,
                    isCommentDisliked,
                    hasReplies,
                    it[Comments.commenterId] == userId
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
            Comments.select(Comments.commenterId eq userId)
                .limit(pageSize).offset(((page - 1) * pageSize).toLong()).map {
                    val commentLikes: Long = getLikesForComment(it[Comments.id])
                    val commentDislikes: Long = getDislikesForComment(it[Comments.id])
                    val lastEdited: LocalDateTime? = getLastCommentEditWithinTransaction(it[Comments.id])
                    val isCommentLiked: Boolean = isCommentLikedByUserWithinTransaction(it[Comments.id], requesterId)
                    val isCommentDisliked: Boolean =
                        isCommentDisLikedByUserWithinTransaction(it[Comments.id], requesterId)
                    val hasReplies = doesCommentHaveReplies(it[Comments.id])
                    val username: String = getUserNameWithinTransaction(it[Comments.commenterId]) ?: "Couldn't load"
                    Comment(
                        it[Comments.id],
                        it[Comments.content],
                        it[Comments.postId],
                        it[Comments.commenterId],
                        username,
                        it[Comments.isReply],
                        it[parentCommentId],
                        it[Comments.timeStamp].toString(),
                        commentLikes,
                        commentDislikes,
                        lastEdited.toString(),
                        isCommentLiked,
                        isCommentDisliked,
                        hasReplies,
                        it[Comments.commenterId] == requesterId
                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

fun getChildComments(commentId: Long, pageSize: Int, page: Int, requesterId: Long?): List<Comment>? {
    return try {
        transaction {
            val parentComment = Comments.select(Comments.id eq commentId).singleOrNull()
            val hasReplies = doesCommentHaveReplies(commentId)
            val parentCommentData = parentComment?.let {
                val username: String = getUserNameWithinTransaction(it[Comments.commenterId]) ?: "Couldn't load"
                Comment(
                    it[Comments.id],
                    it[Comments.content],
                    it[Comments.postId],
                    it[Comments.commenterId],
                    username,
                    it[Comments.isReply],
                    it[parentCommentId],
                    it[Comments.timeStamp].toString(),
                    getLikesForComment(it[Comments.id]),
                    getDislikesForComment(it[Comments.id]),
                    getLastCommentEditWithinTransaction(it[Comments.id]).toString(),
                    isCommentLikedByUserWithinTransaction(it[Comments.id], requesterId),
                    isCommentLikedByUserWithinTransaction(it[Comments.id], requesterId),
                    hasReplies,
                    it[Comments.commenterId] == requesterId
                )
            }

            val childComments = Comments.select(parentCommentId eq commentId)
                .limit(pageSize).offset(((page - 1) * pageSize).toLong()).map {
                    val commentLikes: Long = getLikesForComment(it[Comments.id])
                    val commentDislikes: Long = getDislikesForComment(it[Comments.id])
                    val lastEdited: LocalDateTime? = getLastCommentEditWithinTransaction(it[Comments.id])
                    val isCommentLiked: Boolean = isCommentLikedByUserWithinTransaction(it[Comments.id], requesterId)
                    val isCommentDisliked: Boolean =
                        isCommentDisLikedByUserWithinTransaction(it[Comments.id], requesterId)
                    val hasRepliesChild = doesCommentHaveReplies(it[Comments.id])
                    val username: String = getUserNameWithinTransaction(it[Comments.commenterId]) ?: "Couldn't load"
                    Comment(
                        it[Comments.id],
                        it[Comments.content],
                        it[Comments.postId],
                        it[Comments.commenterId],
                        username,
                        it[Comments.isReply],
                        it[parentCommentId],
                        it[Comments.timeStamp].toString(),
                        commentLikes,
                        commentDislikes,
                        lastEdited.toString(),
                        isCommentLiked,
                        isCommentDisliked,
                        hasRepliesChild,
                        it[Comments.commenterId] == requesterId
                    )
                }

            parentCommentData?.let { listOf(it) + childComments } ?: childComments
        }
    } catch (e: Exception) {
        logger.error { e.message }
    } as List<Comment>?
}