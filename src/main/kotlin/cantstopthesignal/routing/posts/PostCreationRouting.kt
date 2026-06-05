package cantstopthesignal.routing.posts

import com.freedom.cantstopthesignal.database.posts.createPost
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.RetValues
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.ThymeleafContent
import kotlin.text.toIntOrNull

data class PostCreationFields(
    val title: String,
    val topic: String,
    val content: String,
)

fun Application.configurePostCreationRouting() {
    routing {
        authenticate("jwt") {
            get("/posts/create") {
                val model = buildMap {
                    put(
                        ThymeLeafMapKeys.SERVER_CONFIG.value,
                        siteConfig
                    )
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
                    ThymeleafContent("create_post", mapOf(ThymeLeafMapKeys.ERROR.value to "You must provide post contents")
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

                if(success == RetValues.ALREADY_EXISTS.value){

                    val model = buildMap {
                        put(
                            ThymeLeafMapKeys.ERROR.value,
                            "This post already exists, cannot post again"
                        )
                    }

                    return@post call.respond(ThymeleafContent("create_post", model))
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