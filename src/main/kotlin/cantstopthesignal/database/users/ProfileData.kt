package cantstopthesignal.database.users


import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.ProfileData
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
    val profilePhoto: ByteArray?,
    val autoEncrypt: Boolean,
    val createdAt: LocalDateTime,
    val lastLogin: LocalDateTime?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProfileDataEntry

        if (userName != other.userName) return false
        if (bio != other.bio) return false
        if (publicKey != other.publicKey) return false
        if (profilePhoto != null) {
            if (other.profilePhoto == null) return false
            if (!profilePhoto.contentEquals(other.profilePhoto)) return false
        } else if (other.profilePhoto != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userName.hashCode()
        result = 31 * result + (bio?.hashCode() ?: 0)
        result = 31 * result + (publicKey?.hashCode() ?: 0)
        result = 31 * result + (profilePhoto?.contentHashCode() ?: 0)
        return result
    }
}

fun hasAutoEncryptionEnabled(userId: Long): Boolean {
    return try {
        transaction {
            ProfileData.select((ProfileData.userId eq userId) and (ProfileData.autoEncrypt eq true))
                .singleOrNull() != null
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun getPublicKey(userId: Long): String? {
    return try {
        transaction {
            ProfileData.select(ProfileData.userId eq userId)
                .map { it[ProfileData.publicKey] }
                .singleOrNull() // Assuming publicKey is the column name storing public keys
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


fun updateProfilePhoto(userId: Long, photo: ByteArray): Boolean {
    return try {
        transaction {
            ProfileData.update({ ProfileData.userId eq userId }) {
                it[profilePhoto] = ExposedBlob(photo)
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

fun changeAutoEncryptionFlag(userId: Long): Boolean {

    return when {
        doesUserHavePublicKey(userId) -> return try {
            transaction {
                val currentAutoEncrypt = ProfileData.selectAll().where { ProfileData.userId eq userId }
                    .map { it[ProfileData.autoEncrypt] }
                    .singleOrNull() ?: return@transaction false // If no user found, return false

                ProfileData.update({ ProfileData.userId eq userId }) {
                    it[autoEncrypt] = !currentAutoEncrypt
                }
                true
            }
        } catch (e: Exception) {
            logger.error { e.message }
            false
        }

        else -> false
    }
}

fun getProfileDataEntry(userId: Long): ProfileDataEntry? {
    var profileDataEntry: ProfileDataEntry? = null
    try {
        transaction {
            ProfileData.selectAll().where { ProfileData.userId eq userId }.map {
                profileDataEntry = ProfileDataEntry(
                    userName = getUserName(userId) ?: "Could not get username",
                    bio = it[ProfileData.bio] ?: "No bio for this user",
                    publicKey = it[ProfileData.publicKey] ?: "No public key for this user",
                    profilePhoto = it[ProfileData.profilePhoto]?.bytes,
                    autoEncrypt = it[ProfileData.autoEncrypt],
                    createdAt = it[ProfileData.createdAt],
                    lastLogin = it[ProfileData.lastLogin],

                )
            }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        return null
    }

    return profileDataEntry
}