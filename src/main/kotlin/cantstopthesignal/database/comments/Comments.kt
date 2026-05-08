package cantstopthesignal.database.comments

import cantstopthesignal.database.users.getUserName
import cantstopthesignal.database.users.isUserAdmin
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.CommentDislikes
import com.freedom.cantstopthesignal.database.dsl.table_definitions.CommentLikes
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Comments
import org.jetbrains.exposed.v1.core.Count
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDateTime
import kotlin.text.get

data class Comment(
    val id: Long,
    val content: String,
    val postId: Long,
    val commenterId: Long,
    val commenterUsername : String,
    val isReply: Boolean,
    val parentCommentId: Long?,
    val timeStamp: String,
    val commentLikes: Long,
    val commentDislikes: Long,
    val lastEdited: String?,
    val isCommentLikedByMe: Boolean,
    val isCommentDislikedByMe: Boolean,
    val hasReplies: Boolean
)

fun postComment(content: String, commenterId: Long, postId: Long, isReply: Boolean, parentCommentId: Long?): Long? {
    return try {
        transaction {
            Comments.insert {
                it[Comments.content] = content
                it[Comments.postId] = postId
                it[Comments.commenterId] = commenterId
                it[Comments.isReply] = isReply
                it[Comments.parentCommentId] = parentCommentId
                it[timeStamp] = LocalDateTime.now()
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
            comment[Comments.parentCommentId]!!
        }

    } catch (ex: Exception) {
        null
    }
}

fun doesCommentHaveReplies(commentId: Long): Boolean {
    return try {
        transaction {
            Comments.select(Comments.parentCommentId eq commentId).count() > 0
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun getCommentOwnerId(commentId: Long): Long? {
    return try {
        transaction {
            val result = Comments.select(Comments.id eq commentId).singleOrNull()
            result?.get(Comments.commenterId)
        }
    } catch (e: Exception) {
        null
    }
}

fun isIdCommentPoster(userId: Long, commentId: Long): Boolean {
    return try {
        transaction {
            val match = Comments.select((Comments.id eq commentId) and (Comments.commenterId eq userId))
            match.count() > 0
        }
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
            val isCommentDisliked: Boolean = isCommentLikedByUser(id, userId)
            val hasReplies = doesCommentHaveReplies(id)


            Comments.select(Comments.id eq id ).singleOrNull()?.let {
                val username : String = getUserName(it[Comments.commenterId]) ?: "Couldn't load"
                Comment(
                    it[Comments.id],
                    it[Comments.content],
                    it[Comments.postId],
                    it[Comments.commenterId],
                    username,
                    it[Comments.isReply],
                    it[Comments.parentCommentId],
                    it[Comments.timeStamp].toString(),
                    commentLikes,
                    commentDislikes,
                    lastEdited.toString(),
                    isCommentLiked,
                    isCommentDisliked,
                    hasReplies
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
                Comments.parentCommentId,
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
                val lastEdited: LocalDateTime? = getLastCommentEdit(it[Comments.id])
                val isCommentLiked: Boolean = isCommentLikedByUser(it[Comments.id], userId)
                val isCommentDisliked: Boolean = isCommentDisLikedByUser(it[Comments.id], userId)
                val hasReplies = doesCommentHaveReplies(it[Comments.id])
                val username : String = getUserName(it[Comments.commenterId]) ?: "Couldn't load"

                Comment(
                    it[Comments.id],
                    it[Comments.content],
                    it[Comments.postId],
                    it[Comments.commenterId],
                    username,
                    it[Comments.isReply],
                    it[Comments.parentCommentId],
                    it[Comments.timeStamp].toString(),
                    commentLikes,
                    commentDislikes,
                    lastEdited.toString(),
                    isCommentLiked,
                    isCommentDisliked,
                    hasReplies
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
            Comments.select( Comments.commenterId eq userId)
                .limit(pageSize).offset(((page - 1) * pageSize).toLong()).map {
                    val commentLikes: Long = getLikesForComment(it[Comments.id])
                    val commentDislikes: Long = getDislikesForComment(it[Comments.id])
                    val lastEdited: LocalDateTime? = getLastCommentEdit(it[Comments.id])
                    val isCommentLiked: Boolean = isCommentLikedByUser(it[Comments.id], requesterId)
                    val isCommentDisliked: Boolean = isCommentDisLikedByUser(it[Comments.id], requesterId)
                    val hasReplies = doesCommentHaveReplies(it[Comments.id])
                    val username : String = getUserName(it[Comments.commenterId]) ?: "Couldn't load"
                    Comment(
                        it[Comments.id],
                        it[Comments.content],
                        it[Comments.postId],
                        it[Comments.commenterId],
                        username,
                        it[Comments.isReply],
                        it[Comments.parentCommentId],
                        it[Comments.timeStamp].toString(),
                        commentLikes,
                        commentDislikes,
                        lastEdited.toString(),
                        isCommentLiked,
                        isCommentDisliked,
                        hasReplies
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
                val username : String = getUserName(it[Comments.commenterId]) ?: "Couldn't load"
                Comment(
                    it[Comments.id],
                    it[Comments.content],
                    it[Comments.postId],
                    it[Comments.commenterId],
                    username,
                    it[Comments.isReply],
                    it[Comments.parentCommentId],
                    it[Comments.timeStamp].toString(),
                    getLikesForComment(it[Comments.id]),
                    getDislikesForComment(it[Comments.id]),
                    getLastCommentEdit(it[Comments.id]).toString(),
                    isCommentLikedByUser(it[Comments.id], requesterId),
                    isCommentLikedByUser(it[Comments.id], requesterId),
                    hasReplies
                )
            }

            val childComments = Comments.select(Comments.parentCommentId eq commentId)
                .limit(pageSize).offset(((page - 1) * pageSize).toLong()).map {
                    val commentLikes: Long = getLikesForComment(it[Comments.id])
                    val commentDislikes: Long = getDislikesForComment(it[Comments.id])
                    val lastEdited: LocalDateTime? = getLastCommentEdit(it[Comments.id])
                    val isCommentLiked: Boolean = isCommentLikedByUser(it[Comments.id], requesterId)
                    val isCommentDisliked: Boolean = isCommentLikedByUser(it[Comments.id], requesterId)
                    val hasRepliesChild = doesCommentHaveReplies(it[Comments.id])
                    val username : String = getUserName(it[Comments.commenterId]) ?: "Couldn't load"
                    Comment(
                        it[Comments.id],
                        it[Comments.content],
                        it[Comments.postId],
                        it[Comments.commenterId],
                        username,
                        it[Comments.isReply],
                        it[Comments.parentCommentId],
                        it[Comments.timeStamp].toString(),
                        commentLikes,
                        commentDislikes,
                        lastEdited.toString(),
                        isCommentLiked,
                        isCommentDisliked,
                        hasRepliesChild
                    )
                }

            parentCommentData?.let { listOf(it) + childComments } ?: childComments
        }
    } catch (e: Exception) {
        logger.error { e.message }
    } as List<Comment>?
}