package com.freedom.cantstopthesignal.routing

import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.ThymeleafContent

fun Application.configureLoginRoutes() {
    routing {

        get("/login") {
            call.respond(
                ThymeleafContent("login", mapOf<String, Any>())
            )
        }

        post("/login") {
            //do auth and whatnot here
        }

    }
}