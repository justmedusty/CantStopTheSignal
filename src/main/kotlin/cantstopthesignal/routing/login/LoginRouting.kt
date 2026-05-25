package com.freedom.cantstopthesignal.routing

import cantstopthesignal.database.users.getUserId
import cantstopthesignal.database.users.verifyCredentials
import cantstopthesignal.security.JWTConfig
import cantstopthesignal.security.createJWT
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.http.Cookie
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
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
    authenticate("jwt") {

        get("/logout") {
            //We should probably do some invalidation , but it's short lived enough so maybe this is fine
            val model = buildMap {
                put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                put(ThymeLeafMapKeys.SUCCESS.value,"Successfully logged out")
            }

            call.respond(
                ThymeleafContent("login", model)
            )
        }
    }


        post("/login") {
            val params = call.receiveParameters()

            //These two shouldn't be able to happen under normal conditons, but they are thrown a page with the proper error just in case
            val username = params["username"] ?: return@post call.respond(
                ThymeleafContent("login", mapOf(ThymeLeafMapKeys.ERROR.value to "You must provide a username"))
            )

            val password = params["password"] ?: return@post call.respond(
                ThymeleafContent("login", mapOf(ThymeLeafMapKeys.ERROR.value to "You must provide a password"))
            )

            if (!verifyCredentials(username, password)) {
                val model = buildMap {
                    put(
                        ThymeLeafMapKeys.ERROR.value,
                        "Invalid credentials, make sure your user and password are correct."
                    )
                }
                return@post call.respond(
                    ThymeleafContent("login", model)
                )
            }

            val token = (createJWT(
                JWTConfig(
                    "cantstopthesignal",
                    "cantstopthesignal.i2p",
                    System.getenv("JWT_SECRET"),
                    getUserId(username),
                    Length.JWT_TOKEN_LIFETIME_MS.value,
                ),
            ))
            call.response.cookies.append(
                Cookie(name = "jwt", value = token, httpOnly = true, secure = true, path = "/"),
            )
            call.respondRedirect("/feed")


        }
    }


}