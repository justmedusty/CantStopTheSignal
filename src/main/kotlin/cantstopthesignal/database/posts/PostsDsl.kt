package com.freedom.cantstopthesignal.database.posts

import cantstopthesignal.database.posts.*
import cantstopthesignal.database.users.getUserName
import cantstopthesignal.database.users.isUserAdmin
import cantstopthesignal.database.users.isUserSuspended
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.*
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.RetValues
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.ceil


data class Post(
    val id: Long,
    val posterUserName: String,
    val posterId: Long,
    val topic: String,
    val timeStamp: LocalDateTime,
    val title: String,
    val content: String,
    val likeCount: Long,
    val dislikeCount: Long,
    val likedByMe: Boolean,
    val dislikedByMe: Boolean,
    val lastEdited: String?,
    val commentCount: Long,
    val myPost: Boolean, //this can be used to toggle the edit and delete buttons
    val totalPages: Long //Again this causes duplication but this makes life easier with the way things are set up at the time of adding this field
)

/*
    We need to do duplicate checks because if you click the browser refresh page after posting a comment or post it will send the request again
    so this can happen pretty easily. Need to check.
 */
fun isDuplicatePost(userId: Long, content: String, topic: String, title: String): Boolean {
    return try {
        val post = (Posts.selectAll().where { (Posts.topic eq topic) and (Posts.posterId eq userId) }.singleOrNull())
        val postId = post?.get(Posts.id)
        if (postId != null && (PostContents.selectAll()
                .where { (PostContents.postId eq postId) and (PostContents.title eq title) and (PostContents.content eq content) }
                .count() > 0L)
        ) {
            return true

        }
        false
    } catch (e: Exception) {
        logger.error { e.message + " An error occurred trying to check if post was a duplicate" }
        true
    }
}

fun createPost(userId: Long, content: String, topic: String, title: String): Long? {
    return try {
        transaction {
            if (isUserSuspended(userId)) {
                return@transaction null
            }
            if (isDuplicatePost(userId, content, topic, title)) {
                return@transaction RetValues.ALREADY_EXISTS.value
            }
            val postId = insertAndGetId(userId, topic)
            postId != (-1).toLong() && addPostContents(content, postId, title)
            postId
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
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

        val result = Posts.selectAll().where { (Posts.id eq postId) }.singleOrNull()
        result?.get(Posts.posterId)

    } catch (e: Exception) {
        logger.error { e.message + " occurred while getting post owner id" }
        e.printStackTrace()
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
fun totalTopicPages() : Long{
    return try {
        transaction {
            val topics = ceil(Posts
                .select(Posts.topic, Posts.topic.count())
                .groupBy(Posts.topic)
                .orderBy(Posts.topic.count(), SortOrder.DESC).distinct().count().toDouble() / Length.POPULAR_TOPIC_COUNT.value.toDouble()).toLong()
            topics
        }
    }catch (e: Throwable){
        logger.error { "An error occurred ${e.message} while trying to fetch topic pages" }
        0
    }
}
fun fetchPopularTopicNames(page : Long) : List<String>? {
    return try {
        transaction {
            val topics = Posts
                .select(Posts.topic, Posts.topic.count())
                .groupBy(Posts.topic)
                .orderBy(Posts.topic.count(), SortOrder.DESC).offset((page - 1) * Length.POPULAR_TOPIC_COUNT.value)
                .limit(Length.POPULAR_TOPIC_COUNT.value.toInt())
                .map { it[Posts.topic] }
                .toList()
            topics
        }
    }catch (e: Throwable){
        logger.error { "An error occurred ${e.message} while trying to fetch topics" }
        null
    }
}

fun fetchPostsByTopic(
    postTopic: String,
    page: Int,
    limit: Int,
    userId: Long,
    order: String?
): List<Post>? {
    try {
        var orderByCount: Expression<Long>? = null
        var sortOrder: SortOrder = SortOrder.DESC

        when (order) {
            "old" -> sortOrder = SortOrder.ASC
            "new" -> sortOrder = SortOrder.DESC
            "liked" -> orderByCount = PostLikes.postId.count()
            "disliked" -> orderByCount = PostDislikes.postId.count()
            "comments" -> orderByCount = Comments.postId.count()
        }

        return transaction {
            val queryParam = "%$postTopic%"
            val relevantPostIds = Posts.selectAll().where(Posts.topic like queryParam).map { it[Posts.id] }

            val query = Posts.innerJoin(PostContents, { Posts.id }, { PostContents.postId }).leftJoin(PostDislikes)
                .leftJoin(PostLikes).leftJoin(Comments).select(
                    Posts.id, Posts.posterId, Posts.topic, Posts.timestamp, PostContents.title, PostContents.content
                ).where { Posts.id inList relevantPostIds }

            if (orderByCount != null) {
                query.groupBy(Posts.id, PostContents.title, PostContents.content).orderBy(orderByCount, sortOrder)
            } else {
                query.groupBy(Posts.id, PostContents.title, PostContents.content).orderBy(Posts.id, sortOrder)
            }

            query.limit(limit).offset((page - 1) * limit.toLong())

            val totalPages = ceil(query.count().toDouble() / limit.toDouble()).toLong()

            query.map {
                val postId = it[Posts.id]
                val posterUsername = it[Posts.posterId]
                val username = getUserName(posterUsername) ?: "Could not get username"
                val isPostLikedByMe = isPostLikedByUser(postId, userId)
                val isPostDislikedByMe = isPostDislikedByUser(postId, userId)
                val lastEdited = checkLastPostEdit(postId) ?: ""
                val commentCount = Comments.selectAll().where { Comments.postId eq postId }.count()

                Post(
                    postId,
                    username,
                    it[Posts.posterId],
                    it[Posts.topic],
                    it[Posts.timestamp],
                    it[PostContents.title],
                    it[PostContents.content],
                    getLikesForPost(postId),
                    getDislikesForPost(postId),
                    isPostLikedByMe,
                    isPostDislikedByMe,
                    lastEdited.toString(),
                    commentCount,
                    userId == it[Posts.posterId],
                    totalPages,

                    )
            }
        }
    } catch (e: Exception) {
        logger.error { "Error fetching posts: ${e.message}" }
        return null
    }
}

fun fetchPostsFromUser(callerId: Long, page: Int, limit: Int, userId: Long): List<Post>? {
    return try {
        transaction {
            val totalPages = Posts.selectAll().where(Posts.posterId eq userId).count() / limit
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
                    val commentCount = Comments.selectAll().where { Comments.postId eq postId }.count()
                    Post(
                        postId,
                        username,
                        it[Posts.posterId],
                        it[Posts.topic],
                        it[Posts.timestamp],
                        it[PostContents.title],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        isPostLikedByMe,
                        isPostDislikedByMe,
                        lastEdited.toString(),
                        commentCount,
                        it[Posts.posterId] == callerId,
                        totalPages,

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
            val totalPages = Posts.selectAll().where(column eq userId).count() / limit
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
                    val commentCount = Comments.selectAll().where { Comments.postId eq postId }.count()
                    Post(
                        postId,
                        username,
                        it[Posts.posterId],
                        it[Posts.topic],
                        it[Posts.timestamp],
                        it[PostContents.title],
                        it[PostContents.content],
                        getLikesForPost(postId),
                        getDislikesForPost(postId),
                        liked,
                        dislikedByMe,
                        lastEdited.toString(),
                        commentCount,
                        it[Posts.posterId] == userId,
                        totalPages,

                        )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

fun fetchPostById(givenId: Long, userId: Long): List<Post>? {

    return try {
        transaction {
            Posts.innerJoin(PostContents, { id }, { postId }).leftJoin(PostDislikes)
                .leftJoin(PostLikes).select(
                    Posts.id, Posts.posterId, Posts.topic, Posts.timestamp, PostContents.title, PostContents.content
                ).where(Posts.id eq givenId).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername) ?: "Could not get username"
                    val lastEdited = checkLastPostEdit(postId)
                    val dislikedByMe = isPostDislikedByUser(postId, userId)
                    val likedByMe = isPostLikedByUser(postId, userId)
                    val commentCount = Comments.selectAll().where { Comments.postId eq postId }.count()
                    Post(
                        postId,
                        username,
                        it[Posts.posterId],
                        it[Posts.topic],
                        it[Posts.timestamp],
                        it[PostContents.title],
                        it[PostContents.content],
                        getLikesForPost(postId),
                        getDislikesForPost(postId),
                        likedByMe,
                        dislikedByMe,
                        lastEdited.toString(),
                        commentCount,
                        it[Posts.posterId] == userId,
                        0 //Just one post so no point

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
            "comments" -> orderByCount = Comments.postId.count()
            else -> sortOrder = SortOrder.DESC
        }

        return transaction {
            val relevantPostIds = Posts.selectAll().map { it[Posts.id] }

            val query = Posts.innerJoin(PostContents, { id }, { postId }).leftJoin(PostDislikes)
                .leftJoin(PostLikes).leftJoin(Comments).select(
                    Posts.id, Posts.posterId, Posts.topic, Posts.timestamp, PostContents.title, PostContents.content
                ).where { Posts.id inList relevantPostIds }



            if (orderByCount != null) {
                query.groupBy(Posts.id, PostContents.title, PostContents.content).orderBy(orderByCount, sortOrder)
            } else {
                query.groupBy(Posts.id, PostContents.title, PostContents.content).orderBy(Posts.id, sortOrder)
            }

            val totalPages = ceil(query.count().toDouble() / limit.toDouble()).toLong()
            query.limit(limit).offset((page - 1) * limit.toLong())

            query.map {
                val postId = it[Posts.id]
                val posterUsername = it[Posts.posterId]
                val username = getUserName(posterUsername) ?: "Could not get username"
                val isPostLikedByMe = isPostLikedByUser(postId, userId)
                val isPostDislikedByMe = isPostDislikedByUser(postId, userId)
                val lastEdited = checkLastPostEdit(postId)
                val commentCount = Comments.selectAll().where { Comments.postId eq postId }.count()

                Post(
                    postId,
                    username,
                    it[Posts.posterId],
                    it[Posts.topic],
                    it[Posts.timestamp],
                    it[PostContents.title],
                    it[PostContents.content],
                    getLikesForPost(postId),
                    getDislikesForPost(postId),
                    isPostLikedByMe,
                    isPostDislikedByMe,
                    lastEdited.toString(),
                    commentCount,
                    it[Posts.posterId] == userId,
                    totalPages,
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

            val totalPages = ceil(postsWithContents.count().toDouble() / limit.toDouble()).toLong()

            val paginatedQuery =
                postsWithContents.limit(limit).offset((page - 1) * limit.toLong()).orderBy(Posts.id, SortOrder.DESC)

            val posts = paginatedQuery.map { row ->
                val postId = row[Posts.id]
                val posterUserId = row[Posts.posterId]
                val username = getUserName(posterUserId) ?: "Error occurred getting username"
                val isLikedByMe = userId != null && isPostLikedByUser(postId, userId)
                val isDislikedByMe = userId != null && isPostDislikedByUser(postId, userId)
                val lastEdited = checkLastPostEdit(postId)
                val commentCount = Comments.selectAll().where { Comments.postId eq postId }.count()

                Post(
                    postId,
                    username,
                    row[Posts.posterId],
                    row[Posts.topic],
                    row[Posts.timestamp],
                    row[PostContents.title],
                    row[PostContents.content],
                    getLikesForPost(postId),
                    getDislikesForPost(postId),
                    isLikedByMe,
                    isDislikedByMe,
                    lastEdited.toString(),
                    commentCount,
                    row[Posts.id] == userId,
                    totalPages


                )
            }

            posts

        }


    } catch (e: Exception) {
        logger.error { "Error occurred while searching posts: ${e.message}" }
        null
    }

}

/*
    Sanity check helper function to make sure people aren't trying to interact with posts that dont exist
 */
fun verifyPostId(id: Long): Boolean {
    return try {
        transaction {
            Posts.select(Posts.id eq id).count() > 0
        }
    } catch (e: Exception) {
        logger.error { e }
        false
    }
}