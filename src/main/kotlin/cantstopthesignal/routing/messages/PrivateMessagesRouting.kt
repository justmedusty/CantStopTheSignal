package cantstopthesignal.routing.messages

import cantstopthesignal.cryptography.convertPgpMessageOrKey
import cantstopthesignal.cryptography.isPgpMessageOrPgpKey
import cantstopthesignal.database.messages.*
import cantstopthesignal.database.users.getUserId
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.RegexPatterns
import com.freedom.cantstopthesignal.enums.RetValues
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.http.*
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

                val limit: Long = Length.MAX_PAGE_LIMIT.value

                val messageList = getAllConversations(userId!!, page, limit.toInt()) ?: emptyList()


                val model = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.CURRENT_PAGE.value, page)
                    put(ThymeLeafMapKeys.CURRENT_LIMIT.value, limit)
                    put(ThymeLeafMapKeys.PRIVATE_MESSAGE_CONVERSATION.value, messageList)
                }

                return@get call.respond(
                    ThymeleafContent("private_message_list", model)
                )

            }
            get("/messages/conversation/create") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                val model = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                }

                return@get call.respond(
                    ThymeleafContent("create_new_message", model)
                )

            }

            get("/messages/conversations/{id}") {
                val error = call.request.queryParameters["error"]
                val success = call.request.queryParameters["success"]
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                    ?: return@get call.respondRedirect { "/logout" }
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val limit = Length.MAX_PAGE_LIMIT.value.toInt()
                    ?: Length.MAX_PAGE_LIMIT.value.toInt()
                val conversationId = call.parameters["id"]?.toLongOrNull() ?: return@get call.respond(
                    HttpStatusCode.BadRequest
                )

                val messagesList = getMessagesFromConversation(userId, conversationId, page, limit)
                val conversation = getConversation(userId, conversationId)
                val model = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.PRIVATE_MESSAGE_SINGLE_CONVERSATIONS.value, conversation)
                    put(ThymeLeafMapKeys.PRIVATE_MESSAGE_LIST.value, messagesList)
                    /* These can passed in from other errors that could happen which will allow us to do a return@httpmethod call.respondRedirect { /route/uri?error="Error fetching post" }
                    * instead of doing all of that state setup and database queries in a different call, this will clean things up a lot
                    *
                    */
                    if (error != null) {
                        put(ThymeLeafMapKeys.ERROR.value, error)
                    }

                    if (success != null) {
                        put(ThymeLeafMapKeys.SUCCESS.value, success)
                    }
                }

                return@get call.respond(
                    ThymeleafContent("conversation", model)
                )

            }



            post("/messages/conversations/{id}/send") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val parameters = call.receiveParameters()


                val conversationId =
                    call.parameters["id"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                var message = parameters["message"]

                //Make sure conversation is valid
                val verified = verifyConversationId(conversationId, userId!!)

                if (verified == null) {
                    val map = buildMap {
                        put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                        put(ThymeLeafMapKeys.ERROR.value, "The group ID you passed is invalid")
                    }
                    return@post call.respond(ThymeleafContent("create_new_message", map))
                }


                if (message == null || message.length > Length.MAX_DM_MESSAGE_LENGTH.value || message.isBlank()) {
                    val map = buildMap {
                        put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                        put(
                            ThymeLeafMapKeys.ERROR.value,
                            "Your message must be no more than ${Length.MAX_DM_MESSAGE_LENGTH.value} characters and cannot only contain whitespace"
                        )
                    }
                    return@post call.respond(ThymeleafContent("create_new_message", map))
                }

                if (isPgpMessageOrPgpKey(message)) {
                    logger.debug { "post(\"/messages/conversations/{id}/send\"): Converting message $message to valid PGP" }
                    message = convertPgpMessageOrKey(message)
                    logger.debug { " post(\"/messages/conversations/{id}/send\") Message after conversion is $message" }
                }

                val ret = sendMessage(userId!!, conversationId!!, message!!)

                if (ret == null) {
                    val map = buildMap {
                        put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                        put(ThymeLeafMapKeys.ERROR.value, "An error occurred while trying to send your message")
                    }
                    return@post call.respond(ThymeleafContent("create_new_message", map))
                }
                return@post call.respondRedirect("/messages/conversations/$conversationId")


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
                val usernames = usersToAdd?.replace(" ", "")?.split(",")

                if (usernames == null) {
                    val map = buildMap {
                        put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                        put(ThymeLeafMapKeys.ERROR.value, "Users must be specified")
                    }
                    return@post call.respond(ThymeleafContent("create_new_message", map))
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
                        return@post call.respond(ThymeleafContent("create_new_message", map))
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
                        return@post call.respond(ThymeleafContent("create_new_message", map))
                    }

                }

                if ((groupName != null && groupName.length > Length.MAX_GROUPNAME_LENGTH.value)) {
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
                    return@post call.respond(ThymeleafContent("create_new_message", map))
                }
                val userIdList = usernames.map {
                    getUserId(it)!!
                }
                val ret = createConversation(userId!!, userIdList, groupName)

                if (ret == RetValues.ALREADY_EXISTS.value) {
                    val map = buildMap {
                        put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                        put(
                            ThymeLeafMapKeys.ERROR.value,
                            "This conversation already exists, go to conversation view to send messages in it."
                        )
                        put(
                            ThymeLeafMapKeys.PRIVATE_MESSAGE_DRAFT.value,
                            convoDraft
                        ) //We send them back with the same list in case it is long to type out, may as well give them less work to do
                    }
                    return@post call.respond(ThymeleafContent("create_new_message", map))
                }

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
                    return@post call.respond(ThymeleafContent("create_new_message", map))
                }

                //Open the conversation
                return@post call.respondRedirect("/messages/conversations/${ret}")
            }

        }
    }
}