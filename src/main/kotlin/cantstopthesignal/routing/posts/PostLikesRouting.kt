package com.freedom.cantstopthesignal.routing.posts

import cantstopthesignal.database.posts.dislikePost
import cantstopthesignal.database.posts.likePost
import com.freedom.cantstopthesignal.database.posts.verifyPostId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configurePostLikesRouting() {
    routing {
        authenticate("jwt") {

            //The database layer will handle unliking or undisliking etc
            post("/posts/like/{postId}") {
                val postId = call.parameters["postId"]?.toLongOrNull()
                if (postId == null) return@post call.respondRedirect("/posts/$postId?error=Nice try")
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (postId == null) {
                    val error = "The post ID you passed was not correct"
                    return@post call.respondRedirect { "/feed?error=${error}" }
                }
                val validPostId = verifyPostId(postId)
                if (!validPostId) {
                    return@post call.respond(HttpStatusCode.BadRequest)
                }
                val success = likePost(userId!!, postId)
                val successMessage = "Operation successful"
                if (!success) {
                    val errorMessage = "An error occured"
                    return@post call.respondRedirect("/posts/$postId?error=$errorMessage")
                }
                return@post call.respondRedirect("/posts/$postId?success=$successMessage")


            }
            post("/posts/dislike/{postId}") {
                val postId = call.parameters["postId"]?.toLongOrNull()
                if (postId == null) return@post call.respondRedirect("/posts/$postId?error=Nice try")
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (postId == null) {
                    val error = "The post ID you passed was not correct"
                    return@post call.respondRedirect { "/feed?error=${error}" }
                }
                val validPostId = verifyPostId(postId) ?: return@post call.respond(HttpStatusCode.BadRequest)
                val success = dislikePost(userId!!, postId)
                val successMessage = "Operation successful"
                if (!success) {
                    val errorMessage = "An error occurred"
                    return@post call.respondRedirect("/posts/$postId?error=$errorMessage")
                }
                return@post call.respondRedirect("/posts/$postId?success=$successMessage")

            }
        }
    }
}