package com.freedom.cantstopthesignal.routing.comments

import cantstopthesignal.database.comments.dislikeComment
import cantstopthesignal.database.comments.likeComment
import cantstopthesignal.database.comments.verifyCommentId
import com.freedom.cantstopthesignal.database.posts.verifyPostId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureCommentLikesRouting() {
    routing {
        authenticate("jwt") {

            //The database layer will handle unliking or undisliking etc
            post("/comments/{postId}//like/{commentId}") {
                val commentId =
                    call.parameters["commentId"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val postId =
                    call.parameters["postId"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull() ?: return@post call.respond(
                    HttpStatusCode.BadRequest
                )

                val validPostId = verifyPostId(postId)
                if (!validPostId) {
                    return@post call.respond(HttpStatusCode.BadRequest)
                }
                val validCommentId = verifyCommentId(commentId)
                if (!validCommentId) {
                    return@post call.respond(HttpStatusCode.BadRequest)
                }
                val success = likeComment(userId, commentId)
                val successMessage = "Comment liked"
                if (!success) {
                    val errorMessage = "An error occurred"
                    return@post call.respondRedirect("/comments/$postId}/replies/$commentId?error=$errorMessage")
                }
                return@post call.respondRedirect("/comments/$postId/replies/$commentId?success=$successMessage")


            }
            post("/comments/{postId}/dislike/{commentId}") {
                val commentId =
                    call.parameters["commentId"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val postId =
                    call.parameters["postId"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull() ?: return@post call.respond(
                    HttpStatusCode.BadRequest
                )

                val validPostId = verifyPostId(postId)

                if (!validPostId) {
                    return@post call.respond(HttpStatusCode.BadRequest)
                }
                val validCommentId = verifyCommentId(commentId)

                if (!validCommentId) {
                    return@post call.respond(HttpStatusCode.BadRequest)
                }
                val success = dislikeComment(userId, commentId)
                val successMessage = "Comment disliked"
                if (!success) {
                    val errorMessage = "An error occurred"
                    return@post call.respondRedirect("/comments/$postId}/replies/$commentId?error=$errorMessage")
                }
                return@post call.respondRedirect("/comments/$postId/replies/$commentId?success=$successMessage")

            }
        }
    }
}