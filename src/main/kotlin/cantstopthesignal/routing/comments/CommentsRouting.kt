package com.freedom.cantstopthesignal.routing.comments


import cantstopthesignal.database.comments.getCommentsByPost
import cantstopthesignal.database.comments.postComment
import cantstopthesignal.database.notifications.getUnreadNotificationsCount
import cantstopthesignal.database.notifications.numUnreadMessages
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.posts.fetchPostById
import com.freedom.cantstopthesignal.database.posts.verifyPostId
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.RetValues
import com.freedom.cantstopthesignal.enums.SortOrderValues
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*

/*
    This will be put together from database data since these things aren't all stored together
 */


fun Application.configureCommentsRouting() {
    routing {
        authenticate("jwt") {
            /*
                 This route is JUST for top level comments
              */
            post("/comments/post/{postId}") {

                val callingUser = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                //User should never EVER be null and something absolutely catastrophic has happened if it is, but we will check anyway
                if (callingUser == null) {
                    logger.error { "/comments/post/{postId}: User was null! Possible authentication bug or secret leak!" }
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "You are not authorized to perform this operation"
                    )
                }
                /*
                    Since I am going to make the HTML forms require certain fields there shouldn't be any scenarios that I can think of that these things could be missing without
                    someone messing around manually, so I am okay with an obstructive 400 return. For anything that can happen from within normal use we absolutely want to make use of the
                    ThymeLeafMapKeys.ERROR.value mapping with an error message to the page they came from.
                 */
                val params = call.receiveParameters()
                val postId = params["postId"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val commentContents = params["content"] ?: return@post call.respond(HttpStatusCode.BadRequest)


                if (commentContents.isBlank() || commentContents.length > Length.MAX_COMMENT_LENGTH.value || !verifyPostId(
                        postId
                    )
                ) {
                    //This shouldn't be possible under normal conditions so fuck 'em
                    return@post call.respond(HttpStatusCode.BadRequest)
                }

                val result = postComment(commentContents, callingUser, postId, false, null)


                if (result == RetValues.ALREADY_EXISTS.value) {
                    val error = "The comment you tried to post already exists."
                    return@post call.respondRedirect("/comments/$postId?error=$error")
                }

                if (result == null) {
                    val error = "An error occurred while posting your comment"
                    return@post call.respondRedirect("/comments/$postId?error=$error")
                }


                val success = "Comment posted successfully."
                return@post call.respondRedirect("/comments/$postId?success=$success")
            }


            get("/comments/{postId}") {
                val error = call.request.queryParameters["error"]
                val success = call.request.queryParameters["success"]
                val id = call.parameters["postId"]?.toLongOrNull() ?: throw BadRequestException("Invalid or missing id")
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (userId == null) {
                    return@get call.respondRedirect("/logout")
                }

                val page = call.request.queryParameters["page"]?.toInt() ?: 1
                val limit = Length.MAX_PAGE_LIMIT.value.toInt()
                val postList = fetchPostById(id, userId!!)
                val order = call.request.queryParameters["order"] ?: SortOrderValues.NEWEST.value

                if (postList == null) {

                    val model = buildMap {
                        put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                        put(ThymeLeafMapKeys.ERROR.value, "Error fetching post")
                    }
                    return@get call.respond(ThymeleafContent("post", model))
                }

                val post = postList?.get(0)

                val comments = getCommentsByPost(post!!.id, limit, page, userId, order)


                val model = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.POSTS.value, post)
                    put(ThymeLeafMapKeys.COMMENTS.value, comments)
                    put(ThymeLeafMapKeys.NOTIFICATION_COUNT.value, getUnreadNotificationsCount(userId))
                    put(ThymeLeafMapKeys.UNREAD_MESSAGE_COUNT.value, numUnreadMessages(userId))
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