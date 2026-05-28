package cantstopthesignal.routing.messages

import com.freedom.cantstopthesignal.database.posts.fetchPosts
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.ThymeleafContent
import kotlin.text.toInt
import kotlin.text.toIntOrNull


fun Application.configureMessageRouting() {
    routing {
        authenticate("jwt") {
            get("/messages") {

                val model = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                }
                call.respond(
                    ThymeleafContent("private_message_list", model)
                )

            }

        }


    }
}