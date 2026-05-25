package cantstopthesignal.routing

import cantstopthesignal.cryptography.isValidOpenPGPPublicKey
import cantstopthesignal.database.users.User
import cantstopthesignal.database.users.createUser
import cantstopthesignal.database.users.getUserId
import cantstopthesignal.database.users.userNameAlreadyExists
import cantstopthesignal.database.users.verifyCredentials
import cantstopthesignal.security.JWTConfig
import cantstopthesignal.security.createJWT
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.ThymeleafContent

fun Application.configureSignupRoutes() {
    routing {

        get("/signup") {
            call.respond(
                ThymeleafContent("signup", mapOf<String, Any>())
            )
        }

        post("/signup") {
            val params = call.receiveParameters()

            //These two shouldn't be able to happen under normal conditons, but they are thrown a page with the proper error just in case
            val username = params["username"] ?: return@post call.respond(
                ThymeleafContent("login", mapOf(ThymeLeafMapKeys.ERROR.value to "You must provide a username"))
            )

            val password = params["password"] ?: return@post call.respond(
                ThymeleafContent("login", mapOf(ThymeLeafMapKeys.ERROR.value to "You must provide a password"))
            )

            val pgp_publicKey = params["pgp_public_key"]

            if (pgp_publicKey != null && !isValidOpenPGPPublicKey(pgp_publicKey)) {
                call.respond(
                    ThymeleafContent(
                        "signup", mapOf(
                            ThymeLeafMapKeys.ERROR.value to "Your PGP Public key is invalid, please check your PGP key"
                        )
                    )
                )

            }
            val user = User(
                username, pgp_publicKey, password, isAdmin = false, isModerator = false, isSuspended = false
            )

            when {
                username.length !in 6..45 -> {
                    call.respond(
                        ThymeleafContent(
                            "signup", mapOf(
                                ThymeLeafMapKeys.ERROR.value to "Your username must be between 6 and 45 characters"
                            )
                        )
                    )
                }


                password.length !in 8..45 -> {
                    call.respond(
                        ThymeleafContent(
                            "signup", mapOf(
                                ThymeLeafMapKeys.ERROR.value to "Your password must be between 8 and 45 characters"
                            )
                        )
                    )
                }

                userNameAlreadyExists(username) -> {
                    call.respond(
                        ThymeleafContent(
                            "signup", mapOf(
                                ThymeLeafMapKeys.ERROR.value to "Username already exists, please choose another one"
                            )
                        )
                    )
                }

                else -> {
                    if(!createUser(user)){
                        call.respond(
                            ThymeleafContent(
                                "signup", mapOf(
                                    ThymeLeafMapKeys.ERROR.value to "An error occurred while creating your user"
                                )
                            )
                        )
                    }
                    call.respond(
                        ThymeleafContent(
                            "login", mapOf(
                                ThymeLeafMapKeys.SUCCESS.value to "Your account was created successfully"
                            )
                        )
                    )
                }
            }
        }


    }

}
