package com.freedom.cantstopthesignal.routing.posts

import com.freedom.cantstopthesignal.database.posts.verifyPostId
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configurePostRouting() {
    routing {
        authenticate("jwt") {

            //The database layer will handle unliking or undisliking etc
            get("/posts/like/{postId}") {
                val postId = call.parameters["postId"]?.toLongOrNull()
                val userId = call.principal<JWTPrincipal>()?.subject

                if (postId == null) {
                    val error = "The post ID you passed was not correct"
                    return@get call.respondRedirect { "/feed?error=${error}" }
                }
                val validPostId = verifyPostId(postId)


            }
        }
    }
}