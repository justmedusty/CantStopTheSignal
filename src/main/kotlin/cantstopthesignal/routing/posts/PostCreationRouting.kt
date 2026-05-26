package cantstopthesignal.routing.posts

import com.freedom.cantstopthesignal.database.posts.createPost
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

            post("/post/create") {


                val params = call.receiveParameters()


                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if(userId == null){
                    call.respondRedirect("/logout")
                }

                /*
                    Because a post (or a comment) could be fairly long, we will probably want to add conditioning rendering logic in the create post or create comment areas
                    that will allow you to pass a title, topic. contents or comment text back to the template, otherwise these template responses may wipe everythintg they wrote
                    for a minor mistake, I don't know about you but I would be pretty pissed if I wrote a huge post and it wiped it because I left out a topic.
                 */
                val title = params["title"] ?: return@post call.respond(
                    ThymeleafContent("create_post", mapOf(ThymeLeafMapKeys.ERROR.value to "You must provide a title"))
                )

                val topic = params["topic"] ?: return@post call.respond(
                    ThymeleafContent("create_post", mapOf(ThymeLeafMapKeys.ERROR.value to "You must provide a topic"))
                )

                val contents = params["content"] ?: return@post call.respond(
                    ThymeleafContent("create_post", mapOf(ThymeLeafMapKeys.ERROR.value to "You must provide post contents"))
                )

                val success = createPost(userId!!,contents,topic,title)

                if(!success) {

                    val model = buildMap {
                        put(
                            ThymeLeafMapKeys.ERROR.value,
                            "Error creating post"
                        )
                    }

                    call.respond(ThymeleafContent("create_post", model))
                }

                val model = buildMap {
                    put(
                        ThymeLeafMapKeys.SUCCESS.value,
                        "Successfully created post"
                    )
                    put(
                        ThymeLeafMapKeys.SERVER_CONFIG.value,
                        siteConfig
                    )
                }

                call.respond(
                    ThymeleafContent("posts_feed", model)
                )
            }

        }
    }
}