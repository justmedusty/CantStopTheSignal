package cantstopthesignal.routing.messages

import cantstopthesignal.database.messages.getUsersWhoHaveMessagedYou
import cantstopthesignal.database.messages.sendMessage
import cantstopthesignal.database.users.getUserId
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.receiveParameters
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

        post("/messages") {
            val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
            val parameters = call.receiveParameters()

            val sendTo = parameters["sendTo"]
            val message = parameters["message"]

            if(sendTo.isNullOrEmpty()) {
                val map = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.ERROR.value,"User to send message to must be specified")
                }
                call.respond(ThymeleafContent("create_new_message", map))
            }

            val sendToId = getUserId(sendTo!!)

            if(sendToId == null) {
                val map = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.ERROR.value,"The user you are trying to send a message to was not found")
                }
                call.respond(ThymeleafContent("create_new_message", map))
            }

            if (message == null || message.length > Length.MAX_DM_MESSAGE_LENGTH.value || message.isBlank()) {
                val map = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.ERROR.value,"Your message must be no more than ${Length.MAX_DM_MESSAGE_LENGTH.value} characters and cannot only contain whitespace")
                }
                call.respond(ThymeleafContent("create_new_message", map))
            }

            val ret = sendMessage(userId!!,sendToId!!,message!!)

            if(ret == null) {
                val map = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.ERROR.value,"An error occurred while trying to send your message")
                }
                call.respond(ThymeleafContent("create_new_message", map))
            }


            val map = buildMap {
                put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                put(ThymeLeafMapKeys.SUCCESS.value,"Your message was sent")
            }
            call.respond(ThymeleafContent("create_new_message", map))
        }


    }
}