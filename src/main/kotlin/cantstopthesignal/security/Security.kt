package cantstopthesignal.security

import cantstopthesignal.log.logger
import io.ktor.server.application.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.helper.verifyCredentials
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.http.auth.parseAuthorizationHeader
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.thymeleaf.ThymeleafContent

fun Application.configureSecurity() {
    authentication {
        jwt(name = "jwt") {
            verifier(
                JWT.require(Algorithm.HMAC256(System.getenv("JWT_SECRET")))
                    .withIssuer(siteConfig!!.issuer)
                    .build()
            )

            // tell it to read from the cookie instead of the header
            authHeader { call ->
                val token = call.request.cookies["jwt"] ?: return@authHeader null
                try {
                    parseAuthorizationHeader("Bearer $token")
                } catch (e: IllegalArgumentException) {
                    null
                }
            }

            validate { credential ->
                if (credential.payload.subject != null) JWTPrincipal(credential.payload) else null
            }

            challenge { _, _ ->
                val cookie = call.request.cookies["jwt"]
                logger.warn({ "Auth failed — cookie value: $cookie" })
                call.respond(
                    ThymeleafContent("login", mapOf(ThymeLeafMapKeys.ERROR.value to "Session expired or invalid, please log in again."))
                )
            }
        }
    }
    }