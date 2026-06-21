package cantstopthesignal.routing.posts

import cantstopthesignal.database.posts.editPost
import cantstopthesignal.database.posts.fetchPostById
import cantstopthesignal.enums.Length
import cantstopthesignal.enums.ThymeLeafMapKeys
import cantstopthesignal.siteConfig
import io.ktor.http.HttpStatusCode
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
                val userId = call.principal<JWTPrincipal>()!!.subject?.toLongOrNull() ?: return@get call.respond(
                    HttpStatusCode.ExpectationFailed.value
                )
                val id = call.parameters["id"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val post = fetchPostById(id, userId)

                if (post.isNullOrEmpty()) {
                    val errorMessage = "Post not found"
                    return@get call.respond(HttpStatusCode.BadRequest, errorMessage)
                }

                if (userId != post[0].posterId) {
                    val errorMessage = "This is not your post, you cannot edit it."
                    return@get call.respond(HttpStatusCode.BadRequest, errorMessage)
                }


                val model = buildMap {
                    put(
                        ThymeLeafMapKeys.SERVER_CONFIG.value,
                        siteConfig

                    )

                    put(ThymeLeafMapKeys.POSTS.value, post)

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
                    ThymeleafContent("edit_post", model)
                )
            }

            post("/posts/edit/{id}") {
                val params = call.receiveParameters()
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                    ?: return@post call.respondRedirect("/logout")

                val id = call.parameters["id"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)

                val contents = params["content"] ?: return@post call.respondRedirect("/posts/edit/$id?error=You must provide post contents")

                val title = params["title"] ?: return@post call.respondRedirect("/posts/edit/$id?error=You must provide a post title")

                if (title.length > Length.MAX_TITLE_LENGTH.value) {
                  return@post call.respondRedirect("/posts/edit/$id?error=Your title length exceeds ${Length.MAX_TITLE_LENGTH.value} characters")
                }

                if (contents.length > Length.MAX_CONTENT_LENGTH.value) {
                    return@post call.respondRedirect("/posts/edit/$id?error=Your post contents length exceeds ${Length.MAX_CONTENT_LENGTH.value} characters")

                }

                val success = editPost(id, userId, title, contents)

                if (!success) {
                    val error = "Error editing post"
                    return@post call.respondRedirect("/posts/edit/$id?error=$error")
                }


                val successMessage = "Post edited successfully"
                return@post call.respondRedirect("/posts/${id}?success=$successMessage")
            }

        }
    }
}