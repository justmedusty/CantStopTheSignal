package cantstopthesignal.routing.profile


import cantstopthesignal.database.users.ProfileDataEntry
import cantstopthesignal.database.users.getProfileDataEntry
import cantstopthesignal.database.users.getUserId
import cantstopthesignal.database.users.verifyCredentials
import cantstopthesignal.security.JWTConfig
import cantstopthesignal.security.createJWT
import com.freedom.cantstopthesignal.database.dsl.table_definitions.ProfileData
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.ThymeleafContent
import kotlin.collections.mapOf
import kotlin.io.encoding.Base64

fun Application.configureProfileRoutes() {
    routing {
        authenticate("jwt") {
            get("/profile") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                if(userId == null) {
                    call.respond(
                        ThymeleafContent("login", mapOf(ThymeLeafMapKeys.ERROR.value to "Session expired or invalid, please log in again."))
                    )
                }
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