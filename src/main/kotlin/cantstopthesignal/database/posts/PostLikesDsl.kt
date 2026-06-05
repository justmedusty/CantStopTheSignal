package cantstopthesignal.database.posts

import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.PostLikes
import com.freedom.cantstopthesignal.database.dsl.table_definitions.PostLikes.likedById
import com.freedom.cantstopthesignal.enums.Notif
import insertNotification
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun getLikesForPost(postId: Long): Long {
    return try {
        transaction {
            PostLikes.select(
                (PostLikes.postId eq postId)
            ).count()
        }

    } catch (e: Exception) {
        println("Error getting likes for post: $e")
        -1
    }
}

fun isPostLikedByUser(postId: Long, userId: Long): Boolean {
    return try {
        PostLikes.selectAll().where {
            ((PostLikes.postId eq postId) and (likedById eq userId))
        }.count() > 0
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun likePost(likedById: Long, postId: Long): Boolean {
    return try {
        transaction {
            if (isPostDislikedByUser(postId, likedById)) {
                unDislikePost(likedById, postId)
            }
            if (isPostLikedByUser(postId, likedById)) {
                return@transaction unlikePost(likedById, postId)
            }
            PostLikes.insert {
                it[PostLikes.postId] = postId
                it[PostLikes.likedById] = likedById
            }
            insertNotification(postId, null, likedById, Notif.POST_LIKE.value)
            return@transaction true
        }
    } catch (e: Exception) {
        logger.error { e.message + "occurred while trying to like post" }
        false
    }
}

fun isRequesterPostLikeOwner(userId: Long, postId: Long): Boolean {
    return try {
        transaction {
            val match = PostLikes.select((PostLikes.postId eq postId) and (likedById eq userId))
            match.count() > 0
        }
    } catch (e: Exception) {
        logger.error { "Error checking who is comment poster" }
        false
    }
}

fun isRequesterPostLikeOwnerWithTransaction(userId: Long, postId: Long): Boolean {
    return try {
        transaction {
            val match = PostLikes.select((PostLikes.postId eq postId) and (likedById eq userId))
            match.count() > 0
        }
    } catch (e: Exception) {
        logger.error { "Error checking who is comment poster" }
        false
    }
}

fun unlikePost(requesterId: Long, postsId: Long): Boolean {
    try {
        return transaction {
            val success = PostLikes.deleteWhere { (likedById eq requesterId) and (postId eq postsId) }
            success > 0
        }
    } catch (e: Exception) {
        logger.error { e.message }
        return false
    }
}
