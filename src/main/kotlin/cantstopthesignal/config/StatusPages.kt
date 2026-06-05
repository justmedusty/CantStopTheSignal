package cantstopthesignal.config

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            return@exception call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
        }
    }
}
