package cantstopthesignal.routing.posts

import io.ktor.server.application.Application
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.ThymeleafContent

fun Application.configurePostRouting() {
    routing {

        get("/feed") {
            call.respond(
                ThymeleafContent("posts_feed", mapOf<String, Any>())
            )
        }


        get("/post/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException("Invalid or missing id")
            call.respond(
                ThymeleafContent("login", mapOf<String, Any>())
            )
        }


    }
}