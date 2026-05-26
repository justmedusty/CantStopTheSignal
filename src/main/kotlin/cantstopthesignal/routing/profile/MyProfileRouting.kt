package cantstopthesignal.routing.profile


import cantstopthesignal.database.users.ProfileDataEntry
import cantstopthesignal.database.users.getProfileDataEntry
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
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
            get("/profile") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val profile: ProfileDataEntry? = getProfileDataEntry(userId!!)

                val model = buildMap {
                    put(
                        ThymeLeafMapKeys.PROFILE_DATA.value,profile
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