package com.freedom

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.freedom.cantstopthesignal.ThymeleafUser
import io.ktor.server.auth.*
import io.ktor.server.thymeleaf.ThymeleafContent

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello, World!")
        }
        get("/json/jackson") {
                call.respond(mapOf("hello" to "world"))
            }
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
        get("/html-thymeleaf") {
            call.respond(ThymeleafContent("index", mapOf("user" to ThymeleafUser(1, "user1"))))
        }
    }
}