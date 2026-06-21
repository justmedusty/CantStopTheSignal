package cantstopthesignal.routing.profile


import cantstopthesignal.database.notifications.getUnreadNotificationsCount
import cantstopthesignal.database.notifications.numUnreadMessages
import cantstopthesignal.database.users.ProfileDataEntry
import cantstopthesignal.database.users.getProfileDataEntry
import cantstopthesignal.database.users.getUserId
import cantstopthesignal.log.logger
import cantstopthesignal.enums.ThymeLeafMapKeys
import cantstopthesignal.siteConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*

fun Application.configureProfileRoutes() {
    routing {
        authenticate("jwt") {
            get("/profile/{id}") {
                val error = call.request.queryParameters["error"]
                val success = call.request.queryParameters["success"]
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val id = call.parameters["id"]?.toLongOrNull()
                var profile: ProfileDataEntry? = null
                var self = false

                if (id == null) {
                    profile =
                        getProfileDataEntry(userId!!) // This should always be valid , if it isn't it means something very very bad has happened so we do not care to give a nice error message through an html template
                } else {
                    profile = getProfileDataEntry(id)
                }

                if (profile == null) {
                    val model = buildMap {
                        put(
                            ThymeLeafMapKeys.ERROR.value, "An error occurred while fetching this users profile"
                        )
                    }
                    return@get call.respond(ThymeleafContent("profile", model))
                }
                val model = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.PROFILE_DATA.value, profile)
                    put(ThymeLeafMapKeys.UNREAD_MESSAGE_COUNT.value, numUnreadMessages(userId!!))
                    put(ThymeLeafMapKeys.NOTIFICATION_COUNT.value, getUnreadNotificationsCount(userId!!))

                    /* These values can be passed as query params to avoid doing a ton of setup in other call routines, its easier to redirect with a query param instead of duplicating code everywhere */
                    if (error != null) {
                        put(ThymeLeafMapKeys.ERROR.value, error)
                    }

                    if (success != null) {
                        put(ThymeLeafMapKeys.SUCCESS.value, success)
                    }
                }

                self = userId == id

                if (!self) {
                    return@get call.respond(
                        ThymeleafContent("profile", model)
                    )
                } else {
                    return@get call.respond(
                        ThymeleafContent("my_profile", model)
                    )
                }

            }

            get("/profile/name/{username}") {
                val error = call.request.queryParameters["error"]
                val success = call.request.queryParameters["success"]
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val username = call.parameters["username"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val id = getUserId(username)

                if(userId == null) {
                    val error = "No user by the name $username found..."
                    return@get call.respondRedirect { "/feed?error=$error" }
                }

                return@get call.respondRedirect("/profile/$id")

            }

            get("/profile") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()


                val profile =
                    getProfileDataEntry(userId!!) // This should always be valid , if it isn't it means something very very bad has happened so we do not care to give a nice error message through an html template


                if (profile == null) {
                    val model = buildMap {
                        put(
                            ThymeLeafMapKeys.ERROR.value, "An error occurred while fetching this users profile"
                        )
                    }
                    return@get call.respond(ThymeleafContent("profile", model))
                }
                val model = buildMap {
                    put(
                        ThymeLeafMapKeys.PROFILE_DATA.value, profile
                    )
                    put(ThymeLeafMapKeys.UNREAD_MESSAGE_COUNT.value, numUnreadMessages(userId!!))
                    put(ThymeLeafMapKeys.NOTIFICATION_COUNT.value, getUnreadNotificationsCount(userId!!))
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                }

                logger.debug{"PROFILE ISADMIN ${profile.isAdmin} PROFILE ISMOD ${profile.isModerator} PROFILE ISSUSPENDED ${profile.isSuspended}"}

                return@get call.respond(
                    ThymeleafContent("my_profile", model)
                )


            }

            post("/profile/edit") {

            }
        }

    }


}