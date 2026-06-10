package cantstopthesignal.database.users


import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Comments
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Posts
import com.freedom.cantstopthesignal.database.dsl.table_definitions.ProfileData
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Users
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDateTime


data class ProfileDataEntry(
    val userName: String,
    val bio: String?,
    val publicKey: String?,
    val createdAt: LocalDateTime,
    val lastLogin: LocalDateTime?,
    val isAdmin: Boolean,
    val isModerator: Boolean,
    val isSuspended: Boolean,
    val totalPosts: Long,
    val totalComments: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProfileDataEntry

        if (userName != other.userName) return false
        if (bio != other.bio) return false
        if (publicKey != other.publicKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userName.hashCode()
        result = 31 * result + (bio?.hashCode() ?: 0)
        result = 31 * result + (publicKey?.hashCode() ?: 0)
        return result
    }
}

/*
    I am probably not going to implement auto encrypt since it encourages users to let the server encrypt for them.
    Users should always do it themselves, this is the most secure way to do it.

 */

fun getPublicKey(userId: Long): String? {
    return try {
        transaction {
            ProfileData.selectAll().where { (ProfileData.userId eq userId) }.firstOrNull()?.get(ProfileData.publicKey)
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

fun updateBio(userId: Long, bioContents: String): Boolean {
    return try {
        transaction {
            ProfileData.update({ ProfileData.userId eq userId }) {
                it[bio] = bioContents
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun updatePublicKey(userId: Long, keyContents: String): Boolean {
    return try {
        transaction {
            ProfileData.update({ ProfileData.userId eq userId }) {
                it[publicKey] = keyContents
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}


fun doesUserHavePublicKey(userId: Long): Boolean {
    return try {
        ProfileData.selectAll().where { ProfileData.userId eq userId }
            .map { it[ProfileData.publicKey] } // Assuming publicKey is the column name for storing public keys
            .singleOrNull() != null // Check if public key exists for the user
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}


fun getProfileDataEntry(userId: Long): ProfileDataEntry? {
    var profileDataEntry: ProfileDataEntry? = null
    try {
        transaction {
            ProfileData.selectAll().where { ProfileData.userId eq userId }.map {
                val isAdmin = Users.selectAll().where { Users.id eq userId }.firstOrNull()?.get(Users.isAdmin)
                val isModerator = Users.selectAll().where { Users.id eq userId }.firstOrNull()?.get(Users.isModerator)
                val isSuspended = Users.selectAll().where { Users.id eq userId }.firstOrNull()?.get(Users.isSuspended)
                val totalPosts = Posts.selectAll().where{ Posts.posterId eq userId }.count()
                val totalComments = Comments.selectAll().where{ Comments.commenterId eq userId }.count()


                profileDataEntry = ProfileDataEntry(
                    userName = getUserName(userId) ?: "Could not get username",
                    bio = it[ProfileData.bio] ?: "No bio for this user",
                    publicKey = it[ProfileData.publicKey] ?: "No public key for this user",
                    createdAt = it[ProfileData.createdAt],
                    lastLogin = it[ProfileData.lastLogin],
                    isSuspended = isSuspended ?: false,
                    isAdmin = isAdmin ?: false,
                    isModerator = isModerator ?: false,
                    totalPosts = totalPosts,
                    totalComments = totalComments,
                    )
            }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        return null
    }

    return profileDataEntry
}