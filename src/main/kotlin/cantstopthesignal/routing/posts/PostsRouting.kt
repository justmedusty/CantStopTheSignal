package cantstopthesignal.routing.posts

import cantstopthesignal.database.comments.getCommentsByPost
import cantstopthesignal.database.notifications.getUnreadNotificationsCount
import com.freedom.cantstopthesignal.database.posts.fetchPostById
import com.freedom.cantstopthesignal.database.posts.fetchPosts
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.SortOrderValues
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*

/*
    This will be put together from database data since these things aren't all stored together
 */


fun Application.configurePostRouting() {
    routing {
        authenticate("jwt") {
            get("/feed") {
                val page = call.request.queryParameters["page"]?.toInt()?.coerceAtLeast(1) ?: 1
                val limit: Int = Length.MAX_PAGE_LIMIT.value.toInt()
                val sortOrder = call.request.queryParameters["orderBy"] ?: "Desc"
                val callingUser = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val error = call.request.queryParameters["error"]
                val success = call.request.queryParameters["success"]
                val postList = fetchPosts(page, limit, callingUser!!, sortOrder)
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
                    put(ThymeLeafMapKeys.TOTAL_PAGES.value, postList[0].totalPages)
                    put(ThymeLeafMapKeys.CURRENT_PAGE.value,page)
                    put(ThymeLeafMapKeys.NOTIFICATION_COUNT.value, getUnreadNotificationsCount(callingUser))
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


            get("/posts/{id}") {
                val error = call.request.queryParameters["error"]
                val success = call.request.queryParameters["success"]
                val id = call.parameters["id"]?.toLongOrNull() ?: throw BadRequestException("Invalid or missing id")
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (userId == null) {
                    return@get call.respondRedirect("/logout")
                }

                val page = call.request.queryParameters["page"]?.toInt() ?: 1
                val limit = Length.MAX_PAGE_LIMIT.value.toInt()
                val postList = fetchPostById(id, userId!!)
                val order = call.request.queryParameters["order"] ?: SortOrderValues.NEWEST.value

                if (postList == null) {
                    val error = "The post you requested was not found"
                    return@get call.respondRedirect { "/feed/?error=$error" }
                }

                val post = postList?.get(0)

                val comments = getCommentsByPost(post!!.id, limit, page, userId, order)


                val model = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.POSTS.value, post)
                    put(ThymeLeafMapKeys.COMMENTS.value, comments)
                    put(ThymeLeafMapKeys.TOTAL_PAGES.value, if(comments.isNullOrEmpty()) 0 else comments[0].totalPages )
                    put(ThymeLeafMapKeys.CURRENT_PAGE.value, page)
                    put(ThymeLeafMapKeys.NOTIFICATION_COUNT.value, getUnreadNotificationsCount(userId))

                    /* These values can be passed as query params to avoid doing a ton of setup in other call routines, its easier to redirect with a query param instead of duplicating code everywhere */
                    if (error != null) {
                        put(ThymeLeafMapKeys.ERROR.value, error)
                    }

                    if (success != null) {
                        put(ThymeLeafMapKeys.SUCCESS.value, success)
                    }
                }

                return@get call.respond(
                    ThymeleafContent("post", model)
                )
            }


        }
    }
}