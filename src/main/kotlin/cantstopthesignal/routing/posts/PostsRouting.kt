package cantstopthesignal.routing.posts

import com.freedom.cantstopthesignal.database.posts.Post
import com.freedom.cantstopthesignal.database.posts.fetchPosts
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.ThymeleafContent
import org.jetbrains.exposed.v1.core.SortOrder

/*
    This will be put together from database data since these things aren't all stored together
 */


fun Application.configurePostRouting() {
    routing {
        authenticate("jwt") {
            get("/feed") {
                val page = call.request.queryParameters["page"]?.toInt() ?: 1
                val limit =
                    call.request.queryParameters["limit"]?.toInt()?.coerceAtMost(Length.MAX_PAGE_LIMIT.value.toInt())
                        ?: Length.MAX_PAGE_LIMIT.value.toInt()
                val sortOrder = call.request.queryParameters["orderBy"] ?: "Desc"
                val callingUser = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val postList = fetchPosts(page, limit, callingUser!!, sortOrder)
                if (postList == null) {
                    val model = buildMap {
                        put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                        put(ThymeLeafMapKeys.ERROR.value, "An error occurred while fetching posts.")
                    }
                    call.respond(
                        ThymeleafContent("posts_feed", model)
                    )

                }
                val model = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.POSTS.value, postList)
                }
                call.respond(
                    ThymeleafContent("posts_feed", model)
                )
            }


            get("/posts/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException("Invalid or missing id")
                call.respond(
                    ThymeleafContent("login", mapOf<String, Any>())
                )
            }


        }
    }
}