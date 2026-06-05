package cantstopthesignal.routing.profile


import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import getAllNotifications
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*

fun Application.configureNotificationRoutes() {
    routing {
        authenticate("jwt") {
            get("/notifications") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val page = call.request.queryParameters["page"]?.toLongOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toLongOrNull() ?: 50

                val safePage = page.coerceAtLeast(1)
                val safeLimit = limit.coerceIn(1, 100) // max 100 per page

                if (userId == null) {
                    return@get call.respond(
                        ThymeleafContent(
                            "login",
                            mapOf(ThymeLeafMapKeys.ERROR.value to "Session expired or invalid, please log in again.")
                        )
                    )
                }

                //We need to convert this into something usable , its just ids so we will want to generate actual post titles, usernames from userids, etc. It will be for comment likes and post likes, no notifs for dislikes although that would be funny lol
                val list = getAllNotifications(safePage, safeLimit, userId!!)

                val model = buildMap {
                    put(
                        ThymeLeafMapKeys.OTHER_NOTIFICATIONS.value, list
                    )
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.CURRENT_PAGE.value, safePage)
                    put(ThymeLeafMapKeys.CURRENT_LIMIT.value, safeLimit)
                    /*TODO THIS NEEDS TO BE IMPLEMENTED AND NOT LEFT AS A HARDCODED VALUE REMEMBER THIS */
                    put(ThymeLeafMapKeys.TOTAL_PAGES.value, 1)
                }


               return@get call.respond(
                    ThymeleafContent("notifications", model)
                )
            }

        }

    }


}