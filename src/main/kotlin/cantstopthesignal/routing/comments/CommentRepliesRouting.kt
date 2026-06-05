package com.freedom.cantstopthesignal.routing.comments


import cantstopthesignal.database.comments.getCommentById
import cantstopthesignal.database.comments.getPostIdFromComment
import cantstopthesignal.database.comments.getReplyComments
import cantstopthesignal.database.comments.postComment
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.posts.fetchPostById
import com.freedom.cantstopthesignal.database.posts.verifyPostId
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*

/*
    This will be put together from database data since these things aren't all stored together
 */


fun Application.configureCommentRepliesRouting() {
    routing {
        authenticate("jwt") {
            /*
                 This route is JUST for top level comments
              */
            get("/comments/{postId}/replies/{commentId}") {

                val callingUser = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                //User should never EVER be null and something absolutely catastrophic has happened if it is, but we will check anyway
                if (callingUser == null) {
                    logger.error { "/comments/post/{postId}: User was null! Possible authentication bug or secret leak!" }
                    return@get call.respond(HttpStatusCode.BadRequest, "You are not authorized to perform this operation")
                }
                /*
                    Since I am going to make the HTML forms require certain fields there shouldn't be any scenarios that I can think of that these things could be missing without
                    someone messing around manually, so I am okay with an obstructive 400 return. For anything that can happen from within normal use we absolutely want to make use of the
                    ThymeLeafMapKeys.ERROR.value mapping with an error message to the page they came from.
                 */


                var postId =
                    call.parameters["postId"]?.toLongOrNull()
                val commentId =
                    call.parameters["commentId"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val limit = call.parameters["limit"]?.toIntOrNull()?.coerceAtMost(Length.MAX_PAGE_LIMIT.value.toInt())
                    ?: Length.MAX_PAGE_LIMIT.value.toInt()


                if (postId == null) {
                    postId = getPostIdFromComment(commentId) ?: return@get call.respond(HttpStatusCode.BadRequest)
                }



                if (!verifyPostId(postId)) {
                    //This shouldn't be possible under normal conditions so fuck 'em
                    return@get call.respond(HttpStatusCode.BadRequest)
                }

                /*
                    Again, with the HTML templates conditionally rendered so they are designed to not allow users to make calls like this with invalid values,
                    we are fine with just throwing them bad request. This is normally obstructive to use of the service but this shouldn't happen unless
                    someone is handcrafting requests
                 */

                val post = fetchPostById(postId, callingUser!!) ?: return@get call.respond(HttpStatusCode.BadRequest)
                val root_comment =
                    getCommentById(commentId, callingUser) ?: return@get call.respond(HttpStatusCode.BadRequest)
                val replies = getReplyComments(commentId, limit, page, callingUser) ?: return@get call.respond(
                    HttpStatusCode.BadRequest
                )

                val model = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.POSTS.value, post)
                    put(ThymeLeafMapKeys.COMMENT_BEING_REPLIED_TO.value, root_comment)
                    put(ThymeLeafMapKeys.COMMENT_REPLIES.value, replies)
                    put(ThymeLeafMapKeys.POSTS.value, post)
                    put(ThymeLeafMapKeys.CURRENT_PAGE.value, page)
                    put(ThymeLeafMapKeys.CURRENT_LIMIT.value, limit)
                    /* TEMPORARY HARD CODED VALUE THIS NEEDS TO BE GRABBED PROPERLY!*/
                    put(ThymeLeafMapKeys.TOTAL_PAGES.value, 1)
                }
                return@get call.respond(
                    ThymeleafContent("comments", model)
                )
            }

            post("/comments/{commentId}/reply") {

                val callingUser = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                //User should never EVER be null and something absolutely catastrophic has happened if it is, but we will check anyway
                if (callingUser == null) {
                    logger.error { "/comments/post/{postId}: User was null! Possible authentication bug or secret leak!" }
                    return@post call.respond(HttpStatusCode.BadRequest, "You are not authorized to perform this operation")
                }
                /*
                    Since I am going to make the HTML forms require certain fields there shouldn't be any scenarios that I can think of that these things could be missing without
                    someone messing around manually, so I am okay with an obstructive 400 return. For anything that can happen from within normal use we absolutely want to make use of the
                    ThymeLeafMapKeys.ERROR.value mapping with an error message to the page they came from.
                 */

                val params = call.receiveParameters()
                val commentId =
                    params["parentCommentId"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val commentContents = params["content"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                var postId = params["postId"]?.toLongOrNull()

                if (postId == null) {
                    postId = getPostIdFromComment(commentId) ?: return@post call.respond(HttpStatusCode.BadRequest)
                }




                if (commentContents.isBlank() || commentContents.length > Length.MAX_COMMENT_LENGTH.value || !verifyPostId(
                        postId
                    )
                ) {
                    //This shouldn't be possible under normal conditions so fuck 'em
                    return@post call.respond(HttpStatusCode.BadRequest)
                }

                val post = fetchPostById(postId, callingUser!!) ?: return@post call.respond(HttpStatusCode.BadRequest)


                val result =
                    postComment(commentContents, callingUser, postId, true, commentId) ?: return@post call.respond(
                        HttpStatusCode.BadRequest
                    )

                val root_comment =
                    getCommentById(commentId, callingUser) ?: return@post call.respond(HttpStatusCode.BadRequest)
                val replies = getReplyComments(commentId, Length.MAX_PAGE_LIMIT.value.toInt(), 1, callingUser)
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest
                    )


                val model = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.POSTS.value, post)
                    put(ThymeLeafMapKeys.COMMENT_BEING_REPLIED_TO.value, root_comment)
                    put(ThymeLeafMapKeys.COMMENT_REPLIES.value, replies)
                    put(ThymeLeafMapKeys.POSTS.value, post)
                    put(ThymeLeafMapKeys.CURRENT_PAGE.value, 1)
                    put(ThymeLeafMapKeys.CURRENT_LIMIT.value, Length.MAX_COMMENT_LENGTH.value)
                    put(ThymeLeafMapKeys.SUCCESS.value, "Posted commented successfully")
                    /* TEMPORARY HARD CODED VALUE THIS NEEDS TO BE GRABBED PROPERLY!*/
                    put(ThymeLeafMapKeys.TOTAL_PAGES.value, 1)
                }
                return@post call.respond(
                    ThymeleafContent("comments", model)
                )


            }
        }
    }
}