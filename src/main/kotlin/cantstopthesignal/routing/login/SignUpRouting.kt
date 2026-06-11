package cantstopthesignal.routing.login


import cantstopthesignal.cryptography.convertPgpMessageOrKey
import cantstopthesignal.cryptography.isValidOpenPGPPublicKey
import cantstopthesignal.database.invite_only.consumeInviteCode
import cantstopthesignal.database.invite_only.isValidInviteCode
import cantstopthesignal.database.users.User
import cantstopthesignal.database.users.createUser
import cantstopthesignal.database.users.createUserWithoutPassword
import cantstopthesignal.database.users.userNameAlreadyExists
import com.freedom.cantstopthesignal.database.sitewide_permissions.areSignupsSuspended
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.RegexPatterns
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*

fun Application.configureSignupRoutes() {
    routing {

        get("/signup") {
            if (areSignupsSuspended()) {
                return@get call.respond(HttpStatusCode.NotFound)
            }
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
                ThymeleafContent("signup", map)
            )
        }

        post("/signup") {
            if (areSignupsSuspended()) {
                return@post call.respond(HttpStatusCode.NotFound)
            }
            val params = call.receiveParameters()


            //These two shouldn't be able to happen under normal conditons, but they are thrown a page with the proper error just in case
            val username =
                params["username"] ?: return@post call.respondRedirect("/signup?error=You must provide a username")


            val password =
                params["password"]
                    ?: if (siteConfig?.pgpLoginOnly == false) return@post call.respondRedirect("/signup?error=You must provide a password") else null
            val confirmPassword = params["confirm-password"]
                ?: if (siteConfig?.pgpLoginOnly == false) return@post call.respondRedirect("/signup?error=You must confirm your password") else null

            val inviteCode = if (siteConfig?.inviteOnly == true) {
                params["invite-code"]
                    ?: return@post call.respondRedirect("/signup?error=You must provide an invite code")
            } else {
                null
            }

            if (password != confirmPassword) {
                val errorMesage = "Your passwords do not match"
                return@post call.respondRedirect("/signup?error=Your passwords do not match")
            }

            if (siteConfig?.inviteOnly == true) {
                /*
                    We can assert inviteCode isn't null because if invite only is true and the field is null it will have already returned
                 */
                if (!isValidInviteCode(inviteCode!!)) {
                    return@post call.respondRedirect("/signup?error=The invite code you provided is not valid")
                }

            }

            var pgpPublickey = params["pgp"]
                ?: if (siteConfig?.pgpLoginOnly == true) return@post call.respondRedirect("/signup?error=You must provide a valid PGP public key, it is required to be able to log in to your account") else null

            if (!pgpPublickey.isNullOrEmpty() && !isValidOpenPGPPublicKey(pgpPublickey)) {
                return@post call.respondRedirect("/signup?error=The PGP key provided is not valid")
            }
            if (pgpPublickey != null) {
                pgpPublickey = convertPgpMessageOrKey(pgpPublickey)
            }


            val user = User(
                username, pgpPublickey, password, isAdmin = false, isModerator = false, isSuspended = false
            )

            val regex = RegexPatterns.USERNAME.value

            if (!regex.matches(username)) {
                return@post call.respondRedirect("/signup?error=Your username has invalid characters, only letters numbers and underscores are permitted")
            }
            when {
                username.length !in Length.MIN_USERNAME_LENGTH.value..Length.MAX_USERNAME_LENGTH.value -> {
                    return@post call.respondRedirect("/signup?error=Your username must be between ${Length.MIN_USERNAME_LENGTH.value} and ${Length.MAX_USERNAME_LENGTH.value} characters")

                }


                siteConfig?.pgpLoginOnly == false && password!!.length !in Length.MIN_PASSWORD_LENGTH.value..Length.MAX_PASSWORD_LENGTH.value -> {
                    return@post call.respondRedirect("/signupYour password must be between ${Length.MIN_PASSWORD_LENGTH.value} and ${Length.MAX_PASSWORD_LENGTH.value} characters")
                }


                userNameAlreadyExists(username) -> {
                    return@post call.respondRedirect("/signupUsername already exists, please choose another one")
                }

                else -> {
                    if ((siteConfig?.pgpLoginOnly == true && !createUserWithoutPassword(user)) || (siteConfig?.pgpLoginOnly == false && !createUser(
                            user
                        )) || (siteConfig?.inviteOnly == true && !consumeInviteCode(/* We can assert since this has to be evaluted only after invite only is true*/
                            inviteCode!!
                        ))
                    ) {
                        return@post call.respondRedirect("/signupAn error occurred while creating your user")
                    }

                    val success = "Your account was created successfully."
                    return@post call.respondRedirect("/login?success=$success")
                }
            }
        }


    }

}
