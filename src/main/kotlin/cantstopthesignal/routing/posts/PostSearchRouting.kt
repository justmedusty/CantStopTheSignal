package com.freedom.cantstopthesignal.routing.posts

import cantstopthesignal.database.notifications.getUnreadNotificationsCount
import cantstopthesignal.database.notifications.numUnreadMessages
import com.freedom.cantstopthesignal.database.posts.Post
import com.freedom.cantstopthesignal.database.posts.fetchPosts
import com.freedom.cantstopthesignal.database.posts.fetchPostsByTopic
import com.freedom.cantstopthesignal.database.posts.searchPostByTitleOrContents
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
                val searchText = call.request.queryParameters["searchText"] ?: return@get call.respondRedirect("/feed?error=You must specify a search term")
                val searchField = call.request.queryParameters["searchField"] ?: return@get call.respondRedirect("/feed?error=You must specify a search field (topic,title, or contents")
                var sortOrder = call.request.queryParameters["orderBy"] ?: "newest"

                if(sortOrder != "newest" && sortOrder != "old" && sortOrder != "liked" && sortOrder != "disliked" && sortOrder != "comments") {
                    sortOrder = "newest"
                }

                val callingUser = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                val error = call.request.queryParameters["error"]
                val success = call.request.queryParameters["success"]
                var postList : List<Post>? = emptyList()

                when (searchField) {
                    "topic" -> postList = fetchPostsByTopic(searchText, page, limit,callingUser!!,sortOrder)
                    "content" -> postList = searchPostByTitleOrContents(callingUser,searchText,limit,page)
                }


                if (postList == null) {
                    val error = "An error occurred while fetching posts for field ${searchField} with query ${searchText}"
                    return@get call.respondRedirect { call.respondRedirect("/feed?error=$error") }
                }

                val info = when (searchField) {
                    "topic" -> "You are viewing posts by topic ${searchText}"
                    "content" -> "You are viewing posts by content search query \"${searchText}\""
                    else -> null
                }
                val model = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.POSTS.value, postList)
                    put(ThymeLeafMapKeys.TOTAL_PAGES.value, if(postList.isNotEmpty()){postList[0].totalPages} else 1)
                    put(ThymeLeafMapKeys.CURRENT_PAGE.value, page)
                    put(ThymeLeafMapKeys.NOTIFICATION_COUNT.value, getUnreadNotificationsCount(callingUser!!))
                    put(ThymeLeafMapKeys.UNREAD_MESSAGE_COUNT.value, numUnreadMessages(callingUser!!))
                    put(ThymeLeafMapKeys.SEARCH_FIELD.value, searchField)
                    put(ThymeLeafMapKeys.SEARCH_TEXT.value, searchText)
                    put(ThymeLeafMapKeys.SEARCH_INFO.value, info)

                    when (sortOrder) {
                        "liked" -> put(ThymeLeafMapKeys.SORT_ORDER.value, ThymeLeafMapKeys.SORT_ORDER_LIKED.value)
                        "disliked" -> put(ThymeLeafMapKeys.SORT_ORDER.value, ThymeLeafMapKeys.SORT_ORDER_DISLIKED.value)
                        "old" -> put(ThymeLeafMapKeys.SORT_ORDER.value, ThymeLeafMapKeys.SORT_ORDER_OLD.value)
                        "comments" -> put(ThymeLeafMapKeys.SORT_ORDER.value, ThymeLeafMapKeys.SORT_ORDER_COMMENTS.value)
                    }

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
                    ThymeleafContent("posts_search_feed", model)
                )
            }

        }
    }
}