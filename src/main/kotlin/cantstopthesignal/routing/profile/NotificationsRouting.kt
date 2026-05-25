package cantstopthesignal.routing.profile

import getAllNotifications


import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.ThymeleafContent
import kotlin.collections.mapOf

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
                    call.respond(
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
                }


                call.respond(
                    ThymeleafContent("notifications", model)
                )
            }

        }

    }


}