package cantstopthesignal.routing.posts

import cantstopthesignal.database.posts.editPost
import com.freedom.cantstopthesignal.database.posts.createPost
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.RetValues
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.ThymeleafContent

fun Application.configurePostEditingRouting() {
    routing {
        authenticate("jwt") {
            get("/posts/edit/{id}") {
                val error = call.request.queryParameters["error"]
                val success = call.request.queryParameters["success"]
                val model = buildMap {
                    put(
                        ThymeLeafMapKeys.SERVER_CONFIG.value,
                        siteConfig

                    )

                    /*
                    *
                    * These can passed in from other errors that could happen which will allow us to do a return@httpmethod call.respondRedirect { /route/uri?error="Error fetching post" }
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
                    ThymeleafContent("create_post", model)
                )
            }

            post("/posts/edit/{id}") {
                val params = call.receiveParameters()
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (userId == null) {
                    return@post call.respondRedirect("/logout")
                }

                val id = params["id"]?.toLongOrNull() ?: return@post

                val contents = params["content"] ?: return@post call.respond(
                    ThymeleafContent(
                        "edit_post", mapOf(ThymeLeafMapKeys.ERROR.value to "You must provide post contents")
                    )
                )

                val title = params["title"] ?: return@post call.respond(
                    ThymeleafContent(
                        "edit_post", mapOf(ThymeLeafMapKeys.ERROR.value to "You must provide post title")
                    )
                )

                if (title.length > Length.MAX_TITLE_LENGTH.value) {
                    return@post call.respond(
                        ThymeleafContent(
                            "create_post",
                            mapOf(ThymeLeafMapKeys.ERROR.value to "Your title must be less than ${Length.MAX_TITLE_LENGTH.value} characters long}")
                        )
                    )
                }

                if (contents.length > Length.MAX_CONTENT_LENGTH.value) {
                    return@post call.respond(
                        ThymeleafContent(
                            "create_post",
                            mapOf(ThymeLeafMapKeys.ERROR.value to "Your post contents must be less than ${Length.MAX_CONTENT_LENGTH.value} characters long}")
                        )
                    )
                }

                val success = editPost(id, userId, title, contents)

                if (!success) {

                    val model = buildMap {
                        put(
                            ThymeLeafMapKeys.ERROR.value,
                            "Error creating post"
                        )
                    }

                    return@post call.respond(ThymeleafContent("create_post", model))
                }



                return@post call.respondRedirect("/posts/${success}")
            }

        }
    }
}