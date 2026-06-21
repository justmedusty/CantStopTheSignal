package cantstopthesignal.routing.profile


import cantstopthesignal.database.notifications.getAllNotifications
import cantstopthesignal.database.notifications.markAllNotificationsRead
import cantstopthesignal.database.notifications.markNotifRead
import cantstopthesignal.database.notifications.markNotifUnread
import cantstopthesignal.enums.Length
import cantstopthesignal.enums.ThymeLeafMapKeys
import cantstopthesignal.siteConfig
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
                val limit: Long = Length.MAX_PAGE_LIMIT.value
                val error = call.request.queryParameters["error"]
                val success = call.request.queryParameters["success"]

                val safePage = page.coerceAtLeast(1)


                //We need to convert this into something usable , its just ids so we will want to generate actual post titles, usernames from userids, etc. It will be for comment likes and post likes, no notifs for dislikes although that would be funny lol
                val list = getAllNotifications(safePage, limit, userId!!)


                val model = buildMap {
                    put(ThymeLeafMapKeys.OTHER_NOTIFICATIONS.value, list)
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.CURRENT_PAGE.value, safePage)
                    /*TODO THIS NEEDS TO BE IMPLEMENTED AND NOT LEFT AS A HARDCODED VALUE REMEMBER THIS */
                    put(ThymeLeafMapKeys.TOTAL_PAGES.value, 1)
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.TOTAL_PAGES.value, if(list.isNullOrEmpty()) 1 else list[0].numPages)
                    put(ThymeLeafMapKeys.CURRENT_PAGE.value, page ?: 1)

                    /* These can passed in from other errors that could happen which will allow us to do a return@httpmethod call.respondRedirect { /route/uri?error="Error fetching post" }
                    * instead of doing all of that state setup and database queries in a different call, this will clean things up a lot
                    *
                    */
                    if (error != null) {
                        put(ThymeLeafMapKeys.ERROR.value, error)
                    }

                    if (success != null) {
                        put(ThymeLeafMapKeys.SUCCESS.value, success)
                    }
                }


                return@get call.respond(
                    ThymeleafContent("notifications", model)
                )
            }

            get("/notifications/markAllRead") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                val ret = markAllNotificationsRead(userId!!)

                if (!ret) {
                    val error = "Unable to mark all notifications read"
                    return@get call.respondRedirect("/notifications?error=$error")
                }

                val success = "Successfully marked all notifications read"
                call.respondRedirect("/notifications?success=$success")
            }

            get("/notifications/markRead/{id}") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respondRedirect("/notifications?error=Invalid id passed")
                val ret = markNotifRead(id, userId!!)

                if (!ret) {
                    val error = "Unable to mark all notifications read"
                    return@get call.respondRedirect("/notifications?error=$error")
                }

                val success = "Successfully marked notification read"
                call.respondRedirect("/notifications?success=$success")
            }
            get("/notifications/markUnread/{id}") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val id = call.parameters["id"]?.toLongOrNull() ?: return@get call.respondRedirect("/notifications?error=Invalid id passed")

                val ret = markNotifUnread(id,userId!!)

                if (!ret) {
                    val error = "Unable to mark all notifications read"
                    return@get call.respondRedirect("/notifications?error=$error")
                }

                val success = "Successfully marked all notifications read"
                call.respondRedirect("/notifications?success=$success")
            }

        }

    }


}