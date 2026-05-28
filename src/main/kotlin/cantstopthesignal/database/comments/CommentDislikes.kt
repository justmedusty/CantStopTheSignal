package cantstopthesignal.database.comments

import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.CommentDislikes
import com.freedom.cantstopthesignal.database.dsl.table_definitions.CommentLikes
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun isCommentLikedByUserWithinTransaction(commentId: Long, dislikedById: Long?): Boolean {
    return if (dislikedById == null) {
        false
    } else try {
        val alreadyLiked = CommentLikes.select(
            (CommentLikes.commentId eq commentId) and (CommentLikes.likedById eq dislikedById)
        )
        alreadyLiked.count() > 0
    } catch (e: Exception) {
        logger.error { e.message }
        true
    }
}

fun isCommentLikedByUser(commentId: Long, dislikedById: Long?): Boolean {
    return if (dislikedById == null) {
        false
    } else try {
        transaction {

            val alreadyLiked = CommentLikes.select(
                (CommentLikes.commentId eq commentId) and (CommentLikes.likedById eq dislikedById)
            )
            alreadyLiked.count() > 0
        }
    } catch (e: Exception) {
        logger.error { e.message }
        true
    }
}


fun getDislikesForComment(commentId: Long): Long {
    return try {
        CommentDislikes.select(
            (CommentDislikes.commentId eq commentId)
        ).count()
    } catch (e: Exception) {
        logger.error { e.message }
        -1
    }
}

fun dislikeComment(dislikedById: Long, commentId: Long): Boolean {
    if (!isCommentLikedByUser(commentId, dislikedById)) return try {
        transaction {
            CommentDislikes.insert {
                it[CommentDislikes.commentId] = commentId
                it[CommentDislikes.dislikedById] = dislikedById
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
    if (!unlikeComment(dislikedById, commentId)) return false
    return try {
        transaction {
            CommentDislikes.insert {
                it[CommentDislikes.commentId] = commentId
                it[CommentDislikes.dislikedById] = dislikedById
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun isRequesterDislikeOwner(userId: Long, commentId: Long): Boolean {
    return try {
        val match =
            CommentDislikes.select((CommentDislikes.commentId eq commentId) and (CommentDislikes.dislikedById eq userId))
        match.count() > 0
    } catch (e: Exception) {
        logger.error { "Error checking who is comment poster" }
        false
    }
}

fun unDislikeComment(requesterId: Long, commentId: Long): Boolean {
    return try {
        transaction {


            val success =
                CommentDislikes.deleteWhere {
                    (dislikedById eq requesterId) and (CommentDislikes.commentId eq commentId)
                }
            success > 0

        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }


}

fun unDislikeCommentWithinTransaction(requesterId: Long, commentId: Long): Boolean {
    return try {


        val success =
            CommentDislikes.deleteWhere {
                (dislikedById eq requesterId) and (CommentDislikes.commentId eq commentId)
            }
        success > 0

        
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }


}