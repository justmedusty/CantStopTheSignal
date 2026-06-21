package com.freedom.cantstopthesignal.database.admin

import cantstopthesignal.log.logger
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.Comments
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.Posts
import cantstopthesignal.cantstopthesignal.database.dsl.table_definitions.Users
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

data class AdminList(
    val username: String,
)

data class ModeratorList(
    val username: String,
)

data class SiteStats(
    val totalPosts: Long,
    val totalComments: Long,
    val totalUsers: Long,
    val totalSuspendedUsers: Long,
    val adminList: List<AdminList>,
    val moderatorList: List<ModeratorList>,
)


fun getAdminList(): List<AdminList>? {
    return try {
        transaction {
            Users.selectAll().where { Users.isAdmin eq true }.map {
                AdminList(
                    it[Users.userName],
                )
            }
        }
    } catch (e: Exception) {
        logger.error { "${e.message} occurred while fetching admins" }
        null
    }
}

fun getModeratorList(): List<ModeratorList>? {
    return try {
        transaction {
            Users.selectAll().where { Users.isModerator eq true }.map {
                ModeratorList(
                    it[Users.userName],
                )
            }
        }
    } catch (e: Exception) {
        logger.error { "${e.message} occurred while fetching admins" }
        null
    }
}


fun getSiteStats(): SiteStats? {
    return try {
        transaction {
            val userCount = Users.selectAll().count()
            logger.debug { "User count: $userCount" }
            val totalComments = Comments.selectAll().count()
            logger.debug { "Total comments: $totalComments" }
            val totalPosts = Posts.selectAll().count()
            logger.debug { "Total posts: $totalPosts" }
            val totalSuspendedUsers = Users.selectAll().where { Users.isSuspended eq true }.count()
            logger.debug { "Total suspended users: $totalSuspendedUsers" }

            SiteStats(
                totalPosts,
                totalComments,
                userCount,
                totalSuspendedUsers,
                getAdminList() ?: return@transaction null,
                getModeratorList() ?: return@transaction null
            )
        }

    } catch (e: Exception) {
        logger.error { "${e.message} occurred while fetching admins" }
        null
    }
}




