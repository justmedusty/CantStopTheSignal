package cantstopthesignal.routing.comments

import cantstopthesignal.database.comments.getCommentById
import cantstopthesignal.database.comments.updateComment
import cantstopthesignal.enums.Length
import cantstopthesignal.enums.ThymeLeafMapKeys
import cantstopthesignal.siteConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*

fun Application.configureCommentEditRouting() {
    routing {
        authenticate("jwt") {
            get("/comments/edit/{id}") {
                val id = call.parameters["id"]?.toLong() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull() ?: return@get call.respond(
                    HttpStatusCode.Forbidden
                )
                val redirect = call.queryParameters["redirect"] ?: "/feed"
                val error = call.queryParameters["error"]

                val comment = getCommentById(id, user)


                if (comment == null) {
                    val error = "Comment with id $id not found"
                    return@get call.respondRedirect("$redirect?error=$error")
                }

                if (comment.commenterId != user) {
                    return@get call.respond(HttpStatusCode.Forbidden)
                }

                val map = buildMap {
                    put(ThymeLeafMapKeys.COMMENTS.value, comment)
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.REDIRECT_URI.value, redirect)

                    if (error != null) {
                        put(ThymeLeafMapKeys.ERROR.value, error)
                    }
                }

                return@get call.respond(ThymeleafContent("edit_comment", map))

            }

            post("/comments/edit/{id}") {
                val params = call.receiveParameters()
                val id = call.parameters["id"]?.toLong() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull() ?: return@post call.respond(
                    HttpStatusCode.Forbidden
                )
                //we need to do this only to ensure the user is the correct user otherwise you could form handcrafted post requests
                val comment = getCommentById(id, user)
                if (comment == null) {
                    val error = "Comment with id $id not found"
                    return@post call.respond(HttpStatusCode.Forbidden)
                }
                if (comment.commenterId != user) {
                    return@post call.respond(HttpStatusCode.Forbidden)
                }

                val redirect = call.request.queryParameters["redirect"] ?: "/feed"

                val commentContent = params["content"] ?: return@post call.respond(HttpStatusCode.BadRequest)

                if (commentContent.length > Length.MAX_COMMENT_LENGTH.value) {
                    val error =
                        "Comment is too long, must be no larger than ${Length.MAX_COMMENT_LENGTH.value} characters"
                    return@post call.respondRedirect("/comments/edit/${id}?error=$error")
                }

                if (commentContent.isEmpty()) {
                    val error = "Comment content is empty"
                    return@post call.respondRedirect("/comments/edit/${id}?error=$error")
                }

                val ret = updateComment(user, id, commentContent)

                if (!ret) {
                    val error = "An error occurred while trying to update your comment"
                    return@post call.respondRedirect("/comments/edit/${id}?error=$error")
                }

                val success = "Comment successfully updated"
                return@post call.respondRedirect("/comments/edit/${id}?success=$success")


            }
        }
    }

}