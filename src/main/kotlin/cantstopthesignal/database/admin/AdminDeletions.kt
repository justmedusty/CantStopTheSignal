package cantstopthesignal.database.admin

import cantstopthesignal.database.users.getUserName
import cantstopthesignal.enums.Length
import cantstopthesignal.log.logger
import cantstopthesignal.table_definitions.Comments
import cantstopthesignal.table_definitions.Posts
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update


/*
    I do not really want deletions to be much of a thing because it can encourage the very thing that this service is meant to combat, but
    there does need to be some way to handle spam or well poisoning. Suspending + making a service invite only is probably a good way to do it.
    I will decide if I want to implement admin removal at all because in my opinion if something is so bad that it warrants removal the person posting
    it is probably not welcome in the forum anyway.

    I'll let this stew for a bit before making a decision. I am leaning toward making "deleted" a boolean flag to not kill comments or replies that may have been useful to others.
    I will likely just implement soft deletion and maybe allow admins to soft delete, if I do, it will have a clear message saying this was removed by admins for X reason for transparency
 */

fun takeDownPost(postId: Long, adminId: Long, reason: String): Boolean {
    return try {
        transaction {
            Posts.update({ Posts.id eq postId })
            {
                it[Posts.deleted] = true
                it[Posts.deletedReason] =
                    if (reason.length < Length.MAX_REASON_LENGTH.value) reason else reason.substring(
                        0,
                        Length.MAX_REASON_LENGTH.value.toInt()
                    )
            } > 0 && insertAdminLogEntry(adminId,reason,"Admin ${getUserName(adminId)} ID $adminId deleted post $postId for reason: $reason")
        }

    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to take down post $postId , requested by admin $adminId" }
        false
    }

}

fun takeDownComment(commentId: Long, adminId: Long, reason: String): Boolean {
    return try {
        transaction {
            Comments.update({ Comments.id eq commentId })
            {
                it[Comments.deleted] = true
                it[Comments.deletedReason] =
                    if (reason.length < Length.MAX_REASON_LENGTH.value) reason else reason.substring(
                        0,
                        Length.MAX_REASON_LENGTH.value.toInt()
                    )
            } > 0 && insertAdminLogEntry(adminId,reason,"Admin ${getUserName(adminId)} ID $adminId deleted comment $commentId for reason: $reason")
        }

    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to take down post $commentId , requested by admin $adminId" }
        false
    }

}


fun hardDeletePost(postId: Long, adminId: Long, reason: String): Boolean {
    return try {
    transaction {
        Posts.deleteWhere{ Posts.id eq postId } > 0 && insertAdminLogEntry(adminId,reason,"Admin ${getUserName(adminId)} ID $adminId HARD deleted post $postId for reason: $reason")
    }

} catch (e: Exception) {
    logger.error { "${e.message} occurred while trying to hard delete post $postId , requested by admin $adminId" }
    false
}

}

fun hardDeleteComment(commentId: Long, adminId: Long, reason: String): Boolean {
    return try {
        transaction {
            Comments.deleteWhere{ Posts.id eq commentId } > 0 && insertAdminLogEntry(adminId,reason,"Admin ${getUserName(adminId)} ID $adminId HARD deleted comment $commentId for reason: $reason")
        }

    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to hard delete comment $commentId , requested by admin $adminId" }
        false
    }

}
