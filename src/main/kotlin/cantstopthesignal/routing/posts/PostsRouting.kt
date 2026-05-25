package cantstopthesignal.routing.posts

import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.ThymeleafContent

fun Application.configurePostRouting() {
    routing {
        authenticate("jwt") {
            get("/feed") {
                val model = buildMap {
                    put(
                        ThymeLeafMapKeys.SERVER_CONFIG.value,
                        siteConfig
                    )
                }
                call.respond(
                    ThymeleafContent("posts_feed", model)
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
}