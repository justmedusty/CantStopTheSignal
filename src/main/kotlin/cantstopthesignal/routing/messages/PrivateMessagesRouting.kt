package cantstopthesignal.routing.messages

import cantstopthesignal.database.messages.getUsersWhoHaveMessagedYou
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*


fun Application.configureMessageRouting() {
    routing {
        authenticate("jwt") {
            get("/messages") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val page = call.parameters["page"]?.toIntOrNull() ?: 1

                val limit = call.parameters["limit"]?.toIntOrNull()
                    ?.coerceAtMost(Length.MAX_PAGE_LIMIT.value.toInt())
                    ?: Length.MAX_PAGE_LIMIT.value.toInt()

                val messageList = getUsersWhoHaveMessagedYou(userId!!, page, limit) ?: emptyList()


                val model = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.CURRENT_PAGE.value, page)
                    put(ThymeLeafMapKeys.CURRENT_LIMIT.value, limit)
                    put(ThymeLeafMapKeys.PRIVATE_MESSAGE_LIST.value, messageList)
                }

                call.respond(
                    ThymeleafContent("private_message_list", model)
                )

            }

        }


    }
}