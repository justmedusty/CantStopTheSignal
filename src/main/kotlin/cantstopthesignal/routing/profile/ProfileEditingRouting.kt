package cantstopthesignal.routing.profile

import cantstopthesignal.database.users.getProfileDataEntry
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import io.ktor.server.util.getOrFail

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
                }
                return@get call.respond(ThymeleafContent("edit_profile", map))


            }

            get("/profile/edit/username") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
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
                }
                return@get call.respond(ThymeleafContent("edit_profile", map))


            }
            get("/profile/edit/pgp") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
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
                }
                return@get call.respond(ThymeleafContent("edit_profile", map))


            }

            get("/profile/edit/bio") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
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

            }

            post("/profile/edit/username") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                //If userId is not present it should never get here so we can just assert. If this happens it means the cryptographic integrity of your secret is GONE
                val profile = getProfileDataEntry(userId!!)

            }
            post("/profile/edit/pgp") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                //If userId is not present it should never get here so we can just assert. If this happens it means the cryptographic integrity of your secret is GONE
                val profile = getProfileDataEntry(userId!!)
            }

            post("/profile/edit/bio") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                //If userId is not present it should never get here so we can just assert. If this happens it means the cryptographic integrity of your secret is GONE
                val profile = getProfileDataEntry(userId!!)

            }
        }

    }

}

