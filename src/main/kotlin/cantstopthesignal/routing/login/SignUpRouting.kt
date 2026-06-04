package cantstopthesignal.routing.login


import cantstopthesignal.cryptography.convertKey
import cantstopthesignal.cryptography.isValidOpenPGPPublicKey
import cantstopthesignal.database.users.User
import cantstopthesignal.database.users.createUser
import cantstopthesignal.database.users.userNameAlreadyExists
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.RegexPatterns
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*

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
                ThymeleafContent("signup", mapOf(ThymeLeafMapKeys.ERROR.value to "You must provide a username"))
            )

            val password = params["password"] ?: return@post call.respond(
                ThymeleafContent("signup", mapOf(ThymeLeafMapKeys.ERROR.value to "You must provide a password"))
            )

            var pgpPublickey = params["pgp"]

            if (!pgpPublickey.isNullOrEmpty() && !isValidOpenPGPPublicKey(pgpPublickey)) {
                return@post call.respond(
                    ThymeleafContent(
                        "signup", mapOf(
                            ThymeLeafMapKeys.ERROR.value to "Your PGP Public key is invalid, please check your PGP key"
                        )
                    )
                )

            }
            if (pgpPublickey != null) {
                pgpPublickey = convertKey(pgpPublickey)
            }


            val user = User(
                username, pgpPublickey, password, isAdmin = false, isModerator = false, isSuspended = false
            )
            val regex = RegexPatterns.USERNAME.value

            if (!regex.matches(username)) {
                return@post call.respond(
                    ThymeleafContent(
                        "signup", mapOf(
                            ThymeLeafMapKeys.ERROR.value to "Your username has invalid characters, you must ensure you do not have special characters , only letters, numbers, and underscores are permitted"
                        )
                    )
                )
            }
            when {
                username.length !in Length.MIN_USERNAME_LENGTH.value..Length.MAX_USERNAME_LENGTH.value -> {
                    return@post call.respond(
                        ThymeleafContent(
                            "signup", mapOf(
                                ThymeLeafMapKeys.ERROR.value to "Your username must be between ${Length.MIN_USERNAME_LENGTH.value} and ${Length.MAX_USERNAME_LENGTH.value} characters"
                            )
                        )
                    )
                }


                password.length !in Length.MIN_PASSWORD_LENGTH.value..Length.MAX_PASSWORD_LENGTH.value -> {
                    return@post call.respond(
                        ThymeleafContent(
                            "signup", mapOf(
                                ThymeLeafMapKeys.ERROR.value to "Your password must be between ${Length.MIN_PASSWORD_LENGTH.value} and ${Length.MAX_PASSWORD_LENGTH.value} characters"
                            )
                        )
                    )
                }


                userNameAlreadyExists(username) -> {
                    return@post call.respond(
                        ThymeleafContent(
                            "signup", mapOf(
                                ThymeLeafMapKeys.ERROR.value to "Username already exists, please choose another one"
                            )
                        )
                    )
                }

                else -> {
                    if (!createUser(user)) {
                        return@post call.respond(
                            ThymeleafContent(
                                "signup", mapOf(
                                    ThymeLeafMapKeys.ERROR.value to "An error occurred while creating your user"
                                )
                            )
                        )
                    }
                    return@post call.respond(
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
