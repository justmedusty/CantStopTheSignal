package com.freedom.cantstopthesignal.routing.comments

import io.ktor.http.HttpStatusCode


import cantstopthesignal.database.comments.getCommentsByPost
import cantstopthesignal.database.comments.postComment
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.posts.Post
import com.freedom.cantstopthesignal.database.posts.fetchPostById
import com.freedom.cantstopthesignal.database.posts.fetchPosts
import com.freedom.cantstopthesignal.database.posts.verifyPostId
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.RetValues
import com.freedom.cantstopthesignal.enums.SortOrderValues
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.ThymeleafContent
import org.jetbrains.exposed.v1.core.SortOrder

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
                if(callingUser == null){
                    logger.error { "/comments/post/{postId}: User was null! Possible authentication bug or secret leak!" }
                    return@post call.respond(HttpStatusCode.BadRequest, "You are not authorized to perform this operation")
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

                val post = fetchPostById(postId,callingUser!!) ?: return@post call.respond(HttpStatusCode.BadRequest)


                val result = postComment(commentContents, callingUser, postId,false,null)

                val comments = getCommentsByPost(postId, Length.MAX_PAGE_LIMIT.value.toInt(),0,callingUser, SortOrderValues.NEWEST.value)

                if(result == RetValues.ALREADY_EXISTS.value){
                    val model = buildMap {
                        put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                        put(ThymeLeafMapKeys.ERROR.value, "This comment already exists, cannot post it again")
                        put(ThymeLeafMapKeys.POSTS.value, post)
                        put(ThymeLeafMapKeys.COMMENTS.value, comments)
                    }
                    return@post call.respond(ThymeleafContent("post", model))
                }

                if(result == null){
                    val model = buildMap {
                        put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                        put(ThymeLeafMapKeys.ERROR.value, "An error occurred while attempting to post your new comment")
                        put(ThymeLeafMapKeys.POSTS.value, post)
                        put(ThymeLeafMapKeys.COMMENTS.value, comments)
                    }
                    return@post call.respond(ThymeleafContent("post", model))
                }


                val model = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.POSTS.value, post)
                    put(ThymeLeafMapKeys.COMMENTS.value, comments)
                    put(ThymeLeafMapKeys.SUCCESS.value,"Comment successfully posted")
                }
                return@post call.respond(
                    ThymeleafContent("post", model)
                )
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
                val limit =
                    call.request.queryParameters["limit"]?.toInt()?.coerceAtMost(Length.MAX_PAGE_LIMIT.value.toInt())
                        ?: Length.MAX_PAGE_LIMIT.value.toInt()
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