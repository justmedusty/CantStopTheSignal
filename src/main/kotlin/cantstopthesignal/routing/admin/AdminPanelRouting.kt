package com.freedom.cantstopthesignal.routing.admin

import cantstopthesignal.database.users.isUserAdminOrModerator
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*

fun Application.configureAdminRoutes() {
    routing {

        authenticate("jwt") {

            get("/admin") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    return@get call.respond(HttpStatusCode.NotFound)
                }

                val map = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                }

                call.respond(ThymeleafContent("admin_panel", map))
            }
        }


    }


}