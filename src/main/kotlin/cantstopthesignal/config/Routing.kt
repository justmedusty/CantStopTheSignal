package cantstopthesignal.config

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import cantstopthesignal.thymeleaf.ThymeleafUser
import io.ktor.server.thymeleaf.ThymeleafContent

fun Application.configureRouting() {
    routing {
        get("/json/jackson") {
                call.respond(mapOf("hello" to "world"))
            }
        /*
        authenticate("myauth1") {
            get("/protected/route/basic") {
                val principal = call.principal<UserIdPrincipal>()!!
                call.respondText("Hello ${principal.name}")
            }
        }
        authenticate("myauth2") {
            get("/protected/route/form") {
                val principal = call.principal<UserIdPrincipal>()!!
                call.respondText("Hello ${principal.name}")
            }
        }
        */

        get("/html-thymeleaf") {
            call.respond(ThymeleafContent("index", mapOf("user" to ThymeleafUser(1, "user1"))))
        }
    }
}