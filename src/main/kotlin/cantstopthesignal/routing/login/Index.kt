package cantstopthesignal.routing.login


import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.ThymeleafContent

fun Application.indexRouting() {
    routing {

        get("/") {
            call.respond(
                ThymeleafContent("index", mapOf<String, Any>())
            )
        }

    }
}