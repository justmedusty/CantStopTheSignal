package cantstopthesignal.routing.posts

import cantstopthesignal.database.posts.createPost
import cantstopthesignal.enums.Length
import cantstopthesignal.enums.RetValues
import cantstopthesignal.enums.ThymeLeafMapKeys
import cantstopthesignal.siteConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*

data class PostCreationFields(
    val title: String,
    val topic: String,
    val content: String,
)

fun Application.configurePostCreationRouting() {
    routing {
        authenticate("jwt") {
            get("/posts/create") {
                val error = call.request.queryParameters["error"]
                val success = call.request.queryParameters["success"]
                val model = buildMap {
                    put(
                        ThymeLeafMapKeys.SERVER_CONFIG.value,
                        siteConfig

                    )
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
                    ThymeleafContent("create_post", model)
                )
            }

            post("/posts/create") {


                val params = call.receiveParameters()


                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (userId == null) {
                    return@post call.respondRedirect("/logout")
                }

                /*
                    Because a post (or a comment) could be fairly long, we will probably want to add conditional rendering logic in the create post or create comment areas
                    that will allow you to pass a title, topic. contents or comment text back to the template, otherwise these template responses may wipe everythintg they wrote
                    for a minor mistake, I don't know about you but I would be pretty pissed if I wrote a huge post and it wiped it because I left out a topic.
                 */

                /*
                    Regarding the above, I just changed the form so not allow empty or too short or too long fields, so if someone is submitting too small or too large entries it means they're
                    fucking around manually, so we don't care to save their state. That should solve the issue outlined above. They know what they are doing.
                 */

                val title = params["title"] ?: return@post call.respond(
                    ThymeleafContent("create_post", mapOf(ThymeLeafMapKeys.ERROR.value to "You must provide a title"))
                )
                val topic = params["topic"] ?: return@post call.respond(
                    ThymeleafContent("create_post", mapOf(ThymeLeafMapKeys.ERROR.value to "You must provide a topic"))
                )
                val contents = params["content"] ?: return@post call.respond(
                    ThymeleafContent(
                        "create_post", mapOf(ThymeLeafMapKeys.ERROR.value to "You must provide post contents")
                    )
                )


                if (title.length > Length.MAX_TITLE_LENGTH.value) {
                    return@post call.respond(
                        ThymeleafContent(
                            "create_post",
                            mapOf(ThymeLeafMapKeys.ERROR.value to "Your title must be less than ${Length.MAX_TITLE_LENGTH.value} characters long}")
                        )
                    )
                }

                if (topic.length > Length.MAX_TOPIC_LENGTH.value) {
                    return@post call.respond(
                        ThymeleafContent(
                            "create_post",
                            mapOf(ThymeLeafMapKeys.ERROR.value to "Your title must be less than ${Length.MAX_TOPIC_LENGTH.value} characters long}")
                        )
                    )
                }

                if (contents.length > Length.MAX_CONTENT_LENGTH.value) {
                    return@post call.respond(
                        ThymeleafContent(
                            "create_post",
                            mapOf(ThymeLeafMapKeys.ERROR.value to "Your post contents must be less than ${Length.MAX_CONTENT_LENGTH.value} characters long}")
                        )
                    )
                }


                val success = createPost(userId!!, contents, topic, title)

                if (success == RetValues.ALREADY_EXISTS.value) {
                    val error = "This exact post already exists. You cannot create duplicates."
                    return@post call.respondRedirect("/posts/create?error=$error")
                }

                if (success == RetValues.SUSPENDED.value) {
                    val error = "You have been suspended and cannot post. Message a moderator or admin for info."
                    return@post call.respondRedirect("/posts/create?error=$error")
                }


                if (success == null) {

                    val model = buildMap {
                        put(
                            ThymeLeafMapKeys.ERROR.value,
                            "Error creating post"
                        )
                    }

                    return@post call.respond(ThymeleafContent("create_post", model))
                }



                return@post call.respondRedirect("/posts/${success}")
            }

        }
    }
}