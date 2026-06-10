package com.freedom.cantstopthesignal.routing

import cantstopthesignal.database.users.getUserId
import cantstopthesignal.database.users.verifyCredentials
import cantstopthesignal.security.JWTConfig
import cantstopthesignal.security.createJWT
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*

fun Application.configureLoginRoutes() {
    routing {

        get("/login") {
            val error = call.request.queryParameters["error"]
            val success = call.request.queryParameters["success"]


            val map = buildMap {
                put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                /* These values can be passed as query params to avoid doing a ton of setup in other call routines, its easier to redirect with a query param instead of duplicating code everywhere */
                if (error != null) {
                    put(ThymeLeafMapKeys.ERROR.value, error)
                }

                if (success != null) {
                    put(ThymeLeafMapKeys.SUCCESS.value, success)
                }
            }
            return@get call.respond(
                ThymeleafContent("login", map)
            )
        }
        authenticate("jwt") {

            get("/logout") {
                val error = call.request.queryParameters["error"]
                var success = call.request.queryParameters["success"]

                if (success == null && error == null) {
                    success = "Logged out successfully!"
                }
                //We should probably do some invalidation , but it's short lived enough so maybe this is fine
                val model = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    /* These values can be passed as query params to avoid doing a ton of setup in other call routines, its easier to redirect with a query param instead of duplicating code everywhere */
                    if (error != null) {
                        put(ThymeLeafMapKeys.ERROR.value, error)
                    }

                    if (success != null) {
                        put(ThymeLeafMapKeys.SUCCESS.value, success)
                    }

                }

                return@get call.respond(
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
            val model = buildMap {

                put(
                    ThymeLeafMapKeys.SERVER_CONFIG.value,
                    siteConfig
                )
            }


            val token = (createJWT(
                JWTConfig(
                    siteConfig?.audience ?: "someoneisbadanddidntsetthis",
                    siteConfig?.issuer ?: "someoneisbadanddidntsetthis",
                    System.getenv("JWT_SECRET"),
                    getUserId(username)!!, // We can force assert this as not null due to the verifiy credentials call above, it cannot get here if the user info is bogus
                    (siteConfig?.tokenLifetimeMinutes?.times(60)?.times(1000) ?: Length.JWT_TOKEN_LIFETIME_MS.value),
                ),
            ))
            call.response.cookies.append(
                Cookie(name = "jwt", value = token, httpOnly = true, secure = false /* THIS IS FOR I2P ONLY YOU MUST CHANGE THIS IF YOU RUN IT THROUGH TOR OR CLEARNET */ , path = "/"),
            )
            //Redirect user to the home page
            return@post call.respondRedirect("/feed")


        }
    }


}