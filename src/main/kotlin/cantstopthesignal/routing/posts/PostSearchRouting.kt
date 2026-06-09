package com.freedom.cantstopthesignal.routing.posts

import cantstopthesignal.database.notifications.getUnreadNotificationsCount
import cantstopthesignal.database.notifications.numUnreadMessages
import com.freedom.cantstopthesignal.database.posts.fetchPosts
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*

fun Application.configurePostSearchRouting() {
    routing {
        authenticate("jwt") {
            get("/posts/search") {
                val page = call.request.queryParameters["page"]?.toInt()?.coerceAtLeast(1) ?: 1
                val limit: Int = Length.MAX_PAGE_LIMIT.value.toInt()
                val searchText = call.request.queryParameters["searchText"]
                val searchField = call.request.queryParameters["searchField"]
                val callingUser = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val error = call.request.queryParameters["error"]
                val success = call.request.queryParameters["success"]

                val postList = null

                if (postList == null) {
                    val model = buildMap {
                        put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                        put(ThymeLeafMapKeys.ERROR.value, "An error occurred while fetching posts.")
                    }
                    return@get call.respond(
                        ThymeleafContent("posts_feed", model)
                    )

                }
                val model = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.POSTS.value, postList)
                  //  put(ThymeLeafMapKeys.TOTAL_PAGES.value, postList[0].totalPages)
                    put(ThymeLeafMapKeys.CURRENT_PAGE.value, page)
                  //  put(ThymeLeafMapKeys.NOTIFICATION_COUNT.value, getUnreadNotificationsCount(callingUser))
                  //  put(ThymeLeafMapKeys.UNREAD_MESSAGE_COUNT.value, numUnreadMessages(callingUser))

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
                    ThymeleafContent("posts_feed", model)
                )
            }

        }
    }
}