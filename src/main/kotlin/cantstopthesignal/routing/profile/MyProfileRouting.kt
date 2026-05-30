package cantstopthesignal.routing.profile


import cantstopthesignal.database.users.ProfileDataEntry
import cantstopthesignal.database.users.getProfileDataEntry
import cantstopthesignal.database.users.getUserName
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.ThymeleafContent
import kotlin.collections.mapOf

fun Application.configureProfileRoutes() {
    routing {
        authenticate("jwt") {
            get("/profile/{id}") {
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
                    call.respond(ThymeleafContent("profile", model))
                }
                val model = buildMap {
                    put(
                        ThymeLeafMapKeys.PROFILE_DATA.value, profile
                    )
                }

                self = userId == id

                if (!self) {
                    call.respond(
                        ThymeleafContent("profile", model)
                    )
                } else {
                    call.respond(
                        ThymeleafContent("my_profile", model)
                    )
                }

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
                    call.respond(ThymeleafContent("profile", model))
                }
                val model = buildMap {
                    put(
                        ThymeLeafMapKeys.PROFILE_DATA.value, profile
                    )
                }



                call.respond(
                    ThymeleafContent("my_profile", model)
                )


            }

            post("/profile/edit") {

            }
        }

    }


}