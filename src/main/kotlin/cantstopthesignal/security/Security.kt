package cantstopthesignal.security

import cantstopthesignal.siteConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.time.LocalDateTime

val tokenBlackList: HashMap<String, Token> = mutableMapOf<String, Token>() as HashMap<String, Token>

data class Token(
    val subject: String,
    val time: LocalDateTime
)

lateinit var jwtSecret: String

fun Application.configureSecurity() {
    jwtSecret = System.getenv("APP_SECRET_KEY_FILE")
        ?.let { java.io.File(it).readText().trim() }
        ?: error("APP_SECRET_KEY_FILE is required")
    authentication {
        jwt(name = "jwt") {
            verifier(
                JWT.require(Algorithm.HMAC256((jwtSecret)))
                    .withIssuer(siteConfig.issuer)
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
                val error = "Your token has expired or is invalid."
                return@challenge call.respondRedirect("/index?error=${error}")
            }
        }
    }
}