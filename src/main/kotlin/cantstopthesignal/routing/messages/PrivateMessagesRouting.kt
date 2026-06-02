package cantstopthesignal.routing.messages

import cantstopthesignal.database.messages.getAllConversations
import cantstopthesignal.database.messages.sendMessage
import cantstopthesignal.database.messages.verifyConversationId
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
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

                val messageList = getAllConversations(userId!!, page, limit) ?: emptyList()


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

            get("/messages/new") {


                val model = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                }

                call.respond(
                    ThymeleafContent("create_new_message", model)
                )

            }



            post("/messages/send") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val parameters = call.receiveParameters()

                val conversationId = parameters["recipient"]?.toLongOrNull()
                val message = parameters["message"]

                if (conversationId == null) {
                    val map = buildMap {
                        put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                        put(ThymeLeafMapKeys.ERROR.value, "User to send message to must be specified")
                    }
                    call.respond(ThymeleafContent("create_new_message", map))
                }

                val verified = verifyConversationId(conversationId!!)

                if (verified == null) {
                    val map = buildMap {
                        put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                        put(ThymeLeafMapKeys.ERROR.value, "The group ID you passed is invalid")
                    }
                    call.respond(ThymeleafContent("create_new_message", map))
                }


                if (message == null || message.length > Length.MAX_DM_MESSAGE_LENGTH.value || message.isBlank()) {
                    val map = buildMap {
                        put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                        put(
                            ThymeLeafMapKeys.ERROR.value,
                            "Your message must be no more than ${Length.MAX_DM_MESSAGE_LENGTH.value} characters and cannot only contain whitespace"
                        )
                    }
                    call.respond(ThymeleafContent("create_new_message", map))
                }

                val ret = sendMessage(userId!!, conversationId!!, message!!)

                if (ret == null) {
                    val map = buildMap {
                        put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                        put(ThymeLeafMapKeys.ERROR.value, "An error occurred while trying to send your message")
                    }
                    call.respond(ThymeleafContent("create_new_message", map))
                }


                val map = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.SUCCESS.value, "Your message was sent")
                }
                call.respond(ThymeleafContent("create_new_message", map))
            }

        }
    }
}