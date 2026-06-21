package cantstopthesignal.database.comments

import cantstopthesignal.log.logger
import cantstopthesignal.table_definitions.CommentDislikes
import cantstopthesignal.table_definitions.CommentLikes
import cantstopthesignal.enums.Notif
import cantstopthesignal.database.notifications.insertNotification
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun isCommentDisLikedByUser(commentId: Long, likedById: Long?): Boolean {
    return if (likedById == null) {
        false
    } else {
        try {
            transaction {


                val alreadyLiked = CommentDislikes.selectAll().where {
                    (CommentDislikes.commentId eq commentId) and (CommentDislikes.dislikedById eq likedById)
                }
                alreadyLiked.count() > 0
            }
        } catch (e: Exception) {
            logger.error { e.message }
            true
        }
    }

}


fun getLikesForComment(commentId: Long): Long {
    return try {
        CommentLikes.selectAll().where {
            (CommentLikes.commentId eq commentId)
        }.count()
    } catch (e: Exception) {
        logger.error { e.message }
        -1
    }
}

fun likeComment(likedById: Long, commentId: Long): Boolean {

    if (!isCommentDisLikedByUser(commentId, likedById)) return try {
        transaction {
            if(isCommentLikedByUser(commentId, likedById)){
                return@transaction unlikeComment(likedById, commentId)
            }
            CommentLikes.insert {
                it[CommentLikes.commentId] = commentId
                it[CommentLikes.likedById] = likedById
            }
            insertNotification(null, commentId, getCommentOwnerId(commentId)!! ,likedById, Notif.COMMENT_LIKE.value)

            true
        }
    } catch (e: Exception) {
        logger.error { e.message + " occurred while trying to like comment"}
        e.printStackTrace()
        false
    }
    if (!unDislikeComment(likedById, commentId)) return false
    return try {
        transaction {
            CommentLikes.insert {
                it[CommentLikes.commentId] = commentId
                it[CommentLikes.likedById] = likedById
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun isRequesterLikeOwner(userId: Long, commentId: Long): Boolean {
    return try {
        val match =
            CommentLikes.select((CommentLikes.commentId eq commentId) and (CommentLikes.likedById eq userId))
        match.count() > 0
    } catch (e: Exception) {
        logger.error { "Error checking who is comment poster" }
        false
    }
}

fun unlikeComment(requesterId: Long, commentId: Long): Boolean {
    return try {
        transaction {


            val success =
                CommentLikes.deleteWhere { (likedById eq requesterId) and (CommentLikes.commentId eq commentId) }
            success > 0
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

