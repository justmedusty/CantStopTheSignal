package cantstopthesignal.routing.login


import com.freedom.cantstopthesignal.database.sitewide_permissions.isInviteOnlyEnabled
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*

fun Application.indexRouting() {
    routing {

        get("/") {
            return@get call.respondRedirect("/index")
        }


        get("/index") {
            val error = call.request.queryParameters["error"]
            val success = call.request.queryParameters["success"]
            val model = buildMap {
                put(
                    ThymeLeafMapKeys.SERVER_CONFIG.value,
                    siteConfig
                )
                put(ThymeLeafMapKeys.SITE_INVITE_ONLY.value, isInviteOnlyEnabled())
                /* These values can be passed as query params to avoid doing a ton of setup in other call routines, its easier to redirect with a query param instead of duplicating code everywhere */
                if (error != null) {
                    put(ThymeLeafMapKeys.ERROR.value, error)
                }

                if (success != null) {
                    put(ThymeLeafMapKeys.SUCCESS.value, success)
                }
            }
            return@get call.respond(
                ThymeleafContent("index", model)
            )
        }

    }
}