package com.freedom.cantstopthesignal.database.posts

import cantstopthesignal.database.posts.addPostContents
import cantstopthesignal.database.posts.checkLastPostEdit
import cantstopthesignal.database.posts.getDislikesForPost
import cantstopthesignal.database.posts.getLikesForPost
import cantstopthesignal.database.posts.isPostDislikedByUser
import cantstopthesignal.database.posts.isPostLikedByUser
import cantstopthesignal.database.users.getUserName
import cantstopthesignal.database.users.getUserNameWithinTransaction
import cantstopthesignal.database.users.isUserAdmin
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Comments
import com.freedom.cantstopthesignal.database.dsl.table_definitions.PostContents
import com.freedom.cantstopthesignal.database.dsl.table_definitions.PostDislikes
import com.freedom.cantstopthesignal.database.dsl.table_definitions.PostEdits
import com.freedom.cantstopthesignal.database.dsl.table_definitions.PostLikes
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Posts
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime
import java.time.ZoneOffset


data class Post(
    val id: Long,
    val posterUserName: String,
    val topic: String,
    val timeStamp: String,
    val title: String,
    val content: String,
    val likeCount: Long,
    val dislikeCount: Long,
    val likedByMe: Boolean,
    val dislikedByMe: Boolean,
    val lastedEdited: String?,
    val commentCount: Long,
)

fun createPost(userId: Long, content: String, topic: String, title: String): Boolean {
    return try {
        transaction {
            val postId = insertAndGetId(userId, topic)
            postId != (-1).toLong() && addPostContents(content, postId, title)
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun insertAndGetId(poster: Long, postTopic: String): Long {
    return try {
        transaction {
            Posts.insert {
                it[posterId] = poster
                it[topic] = postTopic
                it[timestamp] = LocalDateTime.now(ZoneOffset.UTC)
            } get Posts.id
        }
    } catch (e: Exception) {
        logger.error { }
        -1
    }
}

fun getPostOwnerId(postId: Long): Long? {
    return try {
        transaction {
            val result = Posts.select(Posts.id eq postId).singleOrNull()
            result?.get(Posts.posterId)
        }
    } catch (e: Exception) {
        null
    }
}

fun verifyUserId(userId: Long, postId: Long): Boolean {
    return try {
        transaction {
            Posts.select(
                (Posts.id eq postId) and (Posts.posterId eq userId)

            ).count() > 0
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun deletePost(userId: Long, postId: Long): Boolean {
    return if (verifyUserId(userId, postId) || isUserAdmin(userId)) {
        try {
            transaction {
                Posts.deleteWhere { id eq postId }
                true
            }
        } catch (e: Exception) {
            logger.error { e.message }
            false
        }

    } else {
        false
    }
}

fun fetchPostsByTopic(postTopic: String, page: Int, limit: Int, userId: Long, order: String?): List<Post>? {
    try {
        var orderByCount: Expression<Long>? = null
        var sortOrder: SortOrder = SortOrder.DESC

        when (order) {
            "old" -> sortOrder = SortOrder.ASC
            "new" -> sortOrder = SortOrder.DESC
            "liked" -> orderByCount = PostLikes.postId.count()
            "disliked" -> orderByCount = PostDislikes.postId.count()
        }

        return transaction {
            val queryParam = "%$postTopic%"
            val relevantPostIds = Posts.select(Posts.topic like queryParam).map { it[Posts.id] }

            val query = Posts.innerJoin(PostContents, { id }, { postId }).leftJoin(PostDislikes)
                .leftJoin(PostLikes).select(
                    Posts.id, Posts.posterId, Posts.topic, Posts.timestamp, PostContents.title, PostContents.content
                ).where { Posts.id inList relevantPostIds }

            if (orderByCount != null) {
                query.groupBy(Posts.id, PostContents.title, PostContents.content).orderBy(orderByCount, sortOrder)
            } else {
                query.groupBy(Posts.id, PostContents.title, PostContents.content).orderBy(Posts.id, sortOrder)
            }

            query.limit(limit).offset((page - 1) * limit.toLong())

            query.map {
                val postId = it[Posts.id]
                val posterUsername = it[Posts.posterId]
                val username = getUserNameWithinTransaction(posterUsername) ?: "Could not get username"
                val isPostLikedByMe = isPostLikedByUser(postId, userId)
                val isPostDislikedByMe = isPostDislikedByUser(postId, userId)
                val lastEdited = checkLastPostEdit(postId) ?: ""
                val commentCount = Comments.selectAll().where{Comments.postId eq postId}.count()

                Post(
                    postId,
                    username,
                    it[Posts.topic],
                    it[Posts.timestamp].toString(),
                    it[PostContents.title],
                    it[PostContents.content],
                    getLikesForPost(postId),
                    getDislikesForPost(postId),
                    isPostLikedByMe,
                    isPostDislikedByMe,
                    lastEdited.toString(),
                    commentCount

                )
            }
        }
    } catch (e: Exception) {
        logger.error { "Error fetching posts: ${e.message}" }
        return null
    }
}

fun fetchPostsFromUser(page: Int, limit: Int, userId: Long): List<Post>? {
    return try {
        transaction {
            (Posts innerJoin PostLikes innerJoin PostDislikes innerJoin PostContents leftJoin PostEdits).select(
                Posts.id,
                Posts.posterId,
                Posts.topic,
                PostContents.title,
                Posts.timestamp,
                PostContents.content,
                PostLikes.postId.count(),
                PostDislikes.postId.count()
            ).where(Posts.posterId eq userId).groupBy(Posts.id).orderBy(PostLikes.id.count(), SortOrder.DESC)
                .limit(limit).offset(((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername) ?: return@transaction emptyList<Post>()
                    val isPostLikedByMe = isPostLikedByUser(postId, userId)
                    val isPostDislikedByMe = isPostDislikedByUser(postId, userId)
                    val lastEdited = checkLastPostEdit(postId)
                    val commentCount = Comments.selectAll().where{Comments.postId eq postId}.count()
                    Post(
                        postId,
                        username,
                        it[Posts.topic],
                        it[Posts.timestamp].toString(),
                        it[PostContents.title],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        isPostLikedByMe,
                        isPostDislikedByMe,
                        lastEdited.toString(),
                        commentCount

                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

fun fetchPostsInteractedByMe(page: Int, limit: Int, userId: Long, liked: Boolean): List<Post>? {
    val column: Column<Long> = if (liked) PostLikes.likedById else PostDislikes.dislikedById

    return try {
        transaction {
            Posts.innerJoin(PostContents, { id }, { postId }).leftJoin(PostDislikes)
                .leftJoin(PostLikes).select(
                    Posts.id, Posts.posterId, Posts.topic, Posts.timestamp, PostContents.title, PostContents.content
                ).where(column eq userId).groupBy(Posts.id, PostContents.title, PostContents.content)
                .orderBy(Posts.id, SortOrder.DESC).limit(limit).offset(((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername) ?: "Could not get username"
                    val lastEdited = checkLastPostEdit(postId)
                    val dislikedByMe = !liked
                    val commentCount = Comments.selectAll().where{Comments.postId eq postId}.count()
                    Post(
                        postId,
                        username,
                        it[Posts.topic],
                        it[Posts.timestamp].toString(),
                        it[PostContents.title],
                        it[PostContents.content],
                        getLikesForPost(postId),
                        getDislikesForPost(postId),
                        liked,
                        dislikedByMe,
                        lastEdited.toString(),
                        commentCount

                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

fun fetchPosts(page: Int, limit: Int, userId: Long, order: String?): List<Post>? {
    try {
        var orderByCount: Expression<Long>? = null
        var sortOrder: SortOrder = SortOrder.DESC

        when (order) {
            "old" -> sortOrder = SortOrder.ASC
            "new" -> sortOrder = SortOrder.DESC
            "liked" -> orderByCount = PostLikes.postId.count()
            "disliked" -> orderByCount = PostDislikes.postId.count()
        }

        return transaction {
            val relevantPostIds = Posts.selectAll().map { it[Posts.id] }

            val query = Posts.innerJoin(PostContents, { id }, { postId }).leftJoin(PostDislikes)
                .leftJoin(PostLikes).select(
                    Posts.id, Posts.posterId, Posts.topic, Posts.timestamp, PostContents.title, PostContents.content
                ).where { Posts.id inList relevantPostIds }

            if (orderByCount != null) {
                query.groupBy(Posts.id, PostContents.title, PostContents.content).orderBy(orderByCount, sortOrder)
            } else {
                query.groupBy(Posts.id, PostContents.title, PostContents.content).orderBy(Posts.id, sortOrder)
            }

            query.limit(limit).offset((page - 1) * limit.toLong())
            query.map {
                val postId = it[Posts.id]
                val posterUsername = it[Posts.posterId]
                val username = getUserNameWithinTransaction(posterUsername) ?: "Could not get username"
                val isPostLikedByMe = isPostLikedByUser(postId, userId)
                val isPostDislikedByMe = isPostDislikedByUser(postId, userId)
                val lastEdited = checkLastPostEdit(postId)
                val commentCount = Comments.selectAll().where{Comments.postId eq postId}.count()

                Post(
                    postId,
                    username,
                    it[Posts.topic],
                    it[Posts.timestamp].toString(),
                    it[PostContents.title],
                    it[PostContents.content],
                    getLikesForPost(postId),
                    getDislikesForPost(postId),
                    isPostLikedByMe,
                    isPostDislikedByMe,
                    lastEdited.toString(),
                    commentCount
                )
            }
        }
    } catch (e: Exception) {
        logger.error { "Error fetching posts: ${e.message}" }
        return null
    }
}

fun searchPostByTitleOrContents(userId: Long?, queryParam: String, limit: Int, page: Int): List<Post>? {
    return try {

        transaction {
            val query = "%$queryParam%"
            val postsWithContents = (Posts innerJoin PostContents).select(
                Posts.id, Posts.posterId, Posts.topic, Posts.timestamp, PostContents.title, PostContents.content
            ).where { (PostContents.title like query) or (PostContents.content like query) }

            val paginatedQuery =
                postsWithContents.limit(limit).offset((page - 1) * limit.toLong()).orderBy(Posts.id, SortOrder.DESC)

            val posts = paginatedQuery.map { row ->
                val postId = row[Posts.id]
                val posterUserId = row[Posts.posterId]
                val username = getUserName(posterUserId) ?: "Error occurred getting username"
                val isLikedByMe = userId != null && isPostLikedByUser(postId, userId)
                val isDislikedByMe = userId != null && isPostDislikedByUser(postId, userId)
                val lastEdited = checkLastPostEdit(postId)
                val commentCount = Comments.selectAll().where{Comments.postId eq postId}.count()

                Post(
                    postId,
                    username,
                    row[Posts.topic],
                    row[Posts.timestamp].toString(),
                    row[PostContents.title],
                    row[PostContents.content],
                    getLikesForPost(postId),
                    getDislikesForPost(postId),
                    isLikedByMe,
                    isDislikedByMe,
                    lastEdited.toString(),
                    commentCount

                )
            }

            posts

        }


    } catch (e: Exception) {
        logger.error { "Error occurred while searching posts: ${e.message}" }
        null
    }

}