package cantstopthesignal.routing.comments

import cantstopthesignal.database.comments.getCommentById
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
            get("/comments/{id}/edit") {
                val params = call.receiveParameters()
                val id = call.parameters["id"]?.toLong() ?: return@get call.respond(HttpStatusCode.BadRequest)
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull() ?: return@get call.respond(
                    HttpStatusCode.Forbidden
                )
                val redirect = params["redirect"] ?: "/feed"
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

                    if (error != null) {
                        put(ThymeLeafMapKeys.ERROR.value, error)
                    }
                }

                return@get call.respond(ThymeleafContent("edit_comment", map))

            }

            post("/comments/{id}/edit") {
                val params = call.receiveParameters()
                val id = call.parameters["id"]?.toLong() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull() ?: return@post call.respond(
                    HttpStatusCode.Forbidden
                )


            }
        }
    }

}