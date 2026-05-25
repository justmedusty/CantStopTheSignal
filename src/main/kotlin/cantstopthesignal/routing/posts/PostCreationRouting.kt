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
import kotlin.text.toIntOrNull

fun Application.configurePostCreationRouting() {
    routing {
        authenticate("jwt") {
            get("/post/create") {
                val model = buildMap {
                    put(
                        ThymeLeafMapKeys.SERVER_CONFIG.value,
                        siteConfig
                    )
                }
                call.respond(
                    ThymeleafContent("create_post", model)
                )
            }

        }
    }
}