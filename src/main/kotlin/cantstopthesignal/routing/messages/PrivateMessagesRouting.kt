package cantstopthesignal.routing.messages

import cantstopthesignal.database.messages.createConversation
import cantstopthesignal.database.messages.getAllConversations
import cantstopthesignal.database.messages.sendMessage
import cantstopthesignal.database.messages.verifyConversationId
import cantstopthesignal.database.users.getUserId
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.RegexPatterns
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*

data class ConversationDraft(
    val members: String?,
    val groupName: String?,
)

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
                    put(ThymeLeafMapKeys.PRIVATE_MESSAGE_CONVERSATION.value, messageList)
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

            post("/messages/conversation/create") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val parameters = call.receiveParameters()

                val usersToAdd = parameters["recipients"]
                val groupName = parameters["groupName"]

                val convoDraft = ConversationDraft(
                    usersToAdd,
                    groupName
                )
                //These should ONLY be a comma separating usernames
                val usernames = usersToAdd?.split("\\s*,\\s*")

                if (usernames == null) {
                    val map = buildMap {
                        put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                        put(ThymeLeafMapKeys.ERROR.value, "Users must be specified")
                    }
                    call.respond(ThymeleafContent("create_new_message", map))
                }

                val userRegex = RegexPatterns.USERNAME.value
                for (user in usernames!! /* We can assert not null because we just checked it*/) {

                    //Make sure the username is even valid to begin with
                    if (!userRegex.matches(user) || user.length > Length.MAX_USERNAME_LENGTH.value || user.length < Length.MIN_USERNAME_LENGTH.value) {
                        val map = buildMap {
                            put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                            put(
                                ThymeLeafMapKeys.ERROR.value,
                                "User $user is not valid and thus cannot be a genuine username. Please double check it."
                            )
                            put(
                                ThymeLeafMapKeys.PRIVATE_MESSAGE_DRAFT.value,
                                convoDraft
                            ) //We send them back with the same list in case it is long to type out, may as well give them less work to do
                        }
                        call.respond(ThymeleafContent("create_new_message", map))
                    }

                    /* It can be argued we should only do this check and skip the above check but I think this is more meaningful to the user */
                    if (getUserId(user) == null) {
                        val map = buildMap {
                            put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                            put(
                                ThymeLeafMapKeys.ERROR.value,
                                "User $user was not found in the database, please double check it."
                            )
                            put(
                                ThymeLeafMapKeys.PRIVATE_MESSAGE_DRAFT.value,
                                convoDraft
                            ) //We send them back with the same list in case it is long to type out, may as well give them less work to do
                        }
                        call.respond(ThymeleafContent("create_new_message", map))
                    }

                }

                if (usernames.size > 2 && groupName != null && groupName.length > Length.MAX_GROUPNAME_LENGTH.value) {
                    val map = buildMap {
                        put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                        put(
                            ThymeLeafMapKeys.ERROR.value,
                            "Group name $groupName is too long. Please enter a valid group name. It cannot be longer than ${Length.MAX_GROUPNAME_LENGTH.value} characters."
                        )
                        put(
                            ThymeLeafMapKeys.PRIVATE_MESSAGE_DRAFT.value,
                            convoDraft
                        ) //We send them back with the same list in case it is long to type out, may as well give them less work to do
                    }
                    call.respond(ThymeleafContent("create_new_message", map))
                }
                val userIdList = usernames.map {
                    getUserId(it)!!
                }
                val ret = createConversation(userId!!, userIdList, groupName)

                if (ret == null) {
                    val map = buildMap {
                        put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                        put(
                            ThymeLeafMapKeys.ERROR.value,
                            "An unspecified error occurred while trying to create your new conversation"
                        )
                        put(
                            ThymeLeafMapKeys.PRIVATE_MESSAGE_DRAFT.value,
                            convoDraft
                        ) //We send them back with the same list in case it is long to type out, may as well give them less work to do
                    }
                    call.respond(ThymeleafContent("create_new_message", map))
                }

                //Open the conversation
                call.respondRedirect("/messages/conversation/${ret}")
            }

        }
    }
}