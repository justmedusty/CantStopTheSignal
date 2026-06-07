package cantstopthesignal.routing.profile

import cantstopthesignal.cryptography.convertPgpMessageOrKey
import cantstopthesignal.cryptography.isValidOpenPGPPublicKey
import cantstopthesignal.database.users.getProfileDataEntry
import cantstopthesignal.database.users.getUserName
import cantstopthesignal.database.users.updateBio
import cantstopthesignal.database.users.updatePublicKey
import cantstopthesignal.database.users.updateUserCredentials
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.helper.verifyCredentials
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import io.ktor.server.util.*

data class ProfileEditField(
    val title: String,
    val type: String, //PGP, bio, password etc
)

private const val PasswordTitle = "Edit your password"
private const val UsernameTitle = "Edit your username"
private const val PgpTitle = "Edit your public key"
private const val BioTitle = "Edit your user bio"

private const val PasswordType = "password"
private const val UsernameType = "username"
private const val PgpType = "pgp"
private const val BioType = "bio"


fun Application.configureEditProfileRoutes() {
    routing {
        authenticate("jwt") {
            get("/profile/edit/password") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val error = call.queryParameters["error"]
                val success = call.queryParameters["success"]
                //If userId is not present it should never get here so we can just assert. If this happens it means the cryptographic integrity of your secret is GONE
                val profile = getProfileDataEntry(userId!!)
                val profileEditField = ProfileEditField(
                    title = PasswordTitle,
                    type = PasswordType,
                )
                val map = buildMap {
                    put(ThymeLeafMapKeys.EDIT_FIELD.value, profileEditField)
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.PROFILE_DATA.value, profile)
                    if(error != null) {
                        put(ThymeLeafMapKeys.ERROR.value, error)
                    }
                    if(success != null) {
                        put(ThymeLeafMapKeys.SUCCESS.value, success)
                    }
                }
                return@get call.respond(ThymeleafContent("edit_profile", map))


            }

            get("/profile/edit/username") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val error = call.queryParameters["error"]
                val success = call.queryParameters["success"]
                //If userId is not present it should never get here so we can just assert. If this happens it means the cryptographic integrity of your secret is GONE
                val profile = getProfileDataEntry(userId!!)
                if (profile == null) {
                    /*
                        This should never ever happen but we'll be good programmers and add a check for it
                     */
                    logger.error { "Profile returned null entry with userId = $userId" }
                    return@get call.respond(HttpStatusCode.BadRequest)
                }
                val profileEditField = ProfileEditField(
                    title = UsernameTitle,
                    type = UsernameType,
                )
                val map = buildMap {
                    put(ThymeLeafMapKeys.EDIT_FIELD.value, profileEditField)
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.PROFILE_DATA.value, profile)
                    if(error != null) {
                        put(ThymeLeafMapKeys.ERROR.value, error)
                    }
                    if(success != null) {
                        put(ThymeLeafMapKeys.SUCCESS.value, success)
                    }
                }
                return@get call.respond(ThymeleafContent("edit_profile", map))


            }
            get("/profile/edit/pgp") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val error = call.queryParameters["error"]
                val success = call.queryParameters["success"]
                //If userId is not present it should never get here so we can just assert. If this happens it means the cryptographic integrity of your secret is GONE
                val profile = getProfileDataEntry(userId!!)
                if (profile == null) {
                    /*
                        This should never ever happen but we'll be good programmers and add a check for it
                     */
                    logger.error { "Profile returned null entry with userId = $userId" }
                    return@get call.respond(HttpStatusCode.BadRequest)
                }
                val profileEditField = ProfileEditField(
                    title = PgpTitle,
                    type = PgpType,
                )

                val map = buildMap {
                    put(ThymeLeafMapKeys.EDIT_FIELD.value, profileEditField)
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.PROFILE_DATA.value, profile)
                    if(error != null) {
                        put(ThymeLeafMapKeys.ERROR.value, error)
                    }
                    if(success != null) {
                        put(ThymeLeafMapKeys.SUCCESS.value, success)
                    }
                }
                return@get call.respond(ThymeleafContent("edit_profile", map))


            }

            get("/profile/edit/bio") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val error = call.queryParameters["error"]
                val success = call.queryParameters["success"]
                //If userId is not present it should never get here so we can just assert. If this happens it means the cryptographic integrity of your secret is GONE
                val profile = getProfileDataEntry(userId!!)
                if (profile == null) {
                    /*
                        This should never ever happen but we'll be good programmers and add a check for it
                     */
                    logger.error { "Profile returned null entry with userId = $userId" }
                    return@get call.respond(HttpStatusCode.BadRequest)
                }
                val profileEditField = ProfileEditField(
                    title = BioTitle,
                    type = BioType,
                )
                val map = buildMap {
                    put(ThymeLeafMapKeys.EDIT_FIELD.value, profileEditField)
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.PROFILE_DATA.value, profile)
                    if(error != null) {
                        put(ThymeLeafMapKeys.ERROR.value, error)
                    }
                    if(success != null) {
                        put(ThymeLeafMapKeys.SUCCESS.value, success)
                    }
                }
                return@get call.respond(ThymeleafContent("edit_profile", map))

            }

            post("/profile/edit/password") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                //If userId is not present it should never get here so we can just assert. If this happens it means the cryptographic integrity of your secret is GONE
                val profile = getProfileDataEntry(userId!!)

                val params = call.receiveParameters()


                val currentPassword = params.getOrFail("current_password")
                val newPassword = params.getOrFail("new_password")
                val newPasswordConfirm = params.getOrFail("confirm_password")
                val username = getUserName(userId)!!
                if (!verifyCredentials(username, currentPassword)) {
                    logger.debug { "Passed username is ${getUserName(userId)} and password is $currentPassword" }
                    val error = "Current password does not match your actual current password"
                    return@post call.respondRedirect("/profile/edit/password?error=${error}")
                }

                if (newPassword != newPasswordConfirm) {
                    val error = "Your new password entries are not matching"
                    return@post call.respondRedirect("/profile/edit/password?error=${error}")
                }

                if (newPassword.length > Length.MAX_PASSWORD_LENGTH.value || newPassword.length < Length.MIN_PASSWORD_LENGTH.value) {
                    val error =
                        "Invalid password length, must be between ${Length.MIN_PASSWORD_LENGTH.value} and ${Length.MAX_PASSWORD_LENGTH.value}"
                    return@post call.respondRedirect("/profile/edit/password?error=${error}")
                }

                val ret = updateUserCredentials(userId, true, newPassword)

                if (!ret) {
                    val error = "Password update failed !"
                    return@post call.respondRedirect("/profile/edit/password?success=${error}")

                }
                val success = "Password updated successfully!"
                return@post call.respondRedirect("/profile/edit/password?success=${success}")


            }

            post("/profile/edit/username") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                //If userId is not present it should never get here so we can just assert. If this happens it means the cryptographic integrity of your secret is GONE
                val profile = getProfileDataEntry(userId!!)
                val params = call.receiveParameters()
                val newUsername = params.getOrFail("username")
                if (newUsername.length > Length.MAX_USERNAME_LENGTH.value || newUsername.length < Length.MIN_USERNAME_LENGTH.value) {
                    val error =
                        "Invalid username length, must be between ${Length.MIN_USERNAME_LENGTH.value} and ${Length.MAX_USERNAME_LENGTH.value}"
                    return@post call.respondRedirect("/profile/edit/username?error=${error}")
                }

                val ret = updateUserCredentials(userId, false, newUsername)

                if (!ret) {
                    val error = "Username update failed !"
                    return@post call.respondRedirect("/profile/edit/username?error=${error}")
                }

                val success = "Username updated successfully!"
                return@post call.respondRedirect("/profile/edit/username?success=${success}")


            }
            post("/profile/edit/pgp") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                //If userId is not present it should never get here so we can just assert. If this happens it means the cryptographic integrity of your secret is GONE
                val profile = getProfileDataEntry(userId!!)
                val params = call.receiveParameters()
                val newPgpKey = params.getOrFail("public_key")

                if(!isValidOpenPGPPublicKey(newPgpKey)) {
                    val error = "Invalid public key, please enter a valid PGP key"
                    return@post call.respondRedirect("/profile/edit/pgp?error=${error}")
                }

                val fixedKey = convertPgpMessageOrKey(newPgpKey)

                val ret = updatePublicKey(userId, fixedKey)

                if (!ret) {
                    val error = "An error occurred while updating public key"
                    return@post call.respondRedirect("/profile/edit/pgp?error=${error}")
                }

                val success = "Public key updated successfully!"
                return@post call.respondRedirect("/profile/edit/pgp?success=${success}")
            }

            post("/profile/edit/bio") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                //If userId is not present it should never get here so we can just assert. If this happens it means the cryptographic integrity of your secret is GONE
                val profile = getProfileDataEntry(userId!!)
                val params = call.receiveParameters()
                val newBio = params.getOrFail("bio")

                if (newBio.isEmpty() || newBio.length > Length.MAX_BIO_LENGTH.value) {
                    val error = "Bio must be between 1 and ${Length.MAX_BIO_LENGTH.value} characters"
                    return@post call.respondRedirect("/profile/edit/bio?error=${error}")
                }

                val ret = updateBio(userId, newBio)

                if (!ret) {
                    val error = "Bio update failed !"
                    return@post call.respondRedirect("/profile/edit/bio?error=${error}")
                }

                val success = "Bio updated successfully!"
                return@post call.respondRedirect("/profile/edit/bio?success=${success}")

            }
        }

    }

}

