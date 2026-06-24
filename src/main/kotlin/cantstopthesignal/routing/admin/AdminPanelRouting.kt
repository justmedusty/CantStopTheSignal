package cantstopthesignal.routing.admin

import cantstopthesignal.database.admin.*
import cantstopthesignal.database.invite_only.generateNewInviteCode
import cantstopthesignal.database.invite_only.getAllValidLoginCodes
import cantstopthesignal.database.sitewide_permissions.*
import cantstopthesignal.database.users.*
import cantstopthesignal.enums.Length
import cantstopthesignal.enums.RetStrings
import cantstopthesignal.enums.ThymeLeafMapKeys
import cantstopthesignal.log.logger
import cantstopthesignal.siteConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*

fun Application.configureAdminRoutes() {
    routing {

        authenticate("jwt") {

            get("/admin") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    logger.warn { "User ${getUserName(user)} is not a valid admin user and is attempting to access protected material!" }
                    return@get call.respond(HttpStatusCode.NotFound)
                }

                val success = call.queryParameters["success"]
                val error = call.queryParameters["error"]

                val limit = Length.MAX_PAGE_LIMIT.value.toInt()
                val suspendLogPage = call.queryParameters["suspendLogPage"]?.toIntOrNull() ?: 1
                val suspendLogsOrder = call.queryParameters["suspendLogsOrder"]
                val adminLogsPage = call.queryParameters["adminLogsPage"]?.toIntOrNull() ?: 1
                val adminLogsPageOrder = call.queryParameters["adminLogsOrder"]

                val motd = getMotd()
                val infoMessage = getInfoMessage()

                val suspendLogs =
                    getSuspendLogEntries(page = suspendLogPage, limit = limit, userId = user, order = suspendLogsOrder)
                val adminLogs =
                    getAdminLogEntries(page = adminLogsPage, limit = limit, userId = user, order = adminLogsPageOrder)

                val siteStats = getSiteStats()

                val areSignupsSuspended = areSignupsSuspended()
                val inviteOnly = isInviteOnlyEnabled()

                val inviteCodes: List<String>? = getAllValidLoginCodes(
                    user, 1, 100
                )//Im just gonna show 100, you should never need to page through these so don't think I'll implement it. If you need to give out an invite code, why would you page through them? Just pick one.


                val map = buildMap {
                    put(ThymeLeafMapKeys.SERVER_CONFIG.value, siteConfig)
                    put(ThymeLeafMapKeys.ADMIN_INVITE_ONLY.value, inviteOnly)
                    put(ThymeLeafMapKeys.ADMIN_ARE_SIGNUPS_SUSPENDED.value, areSignupsSuspended)
                    put(ThymeLeafMapKeys.ADMIN_INFO_MESSAGE.value, infoMessage)
                    put(ThymeLeafMapKeys.ADMIN_MOTD_MESSAGE.value, motd)
                    put(ThymeLeafMapKeys.ADMIN_SUSPEND_LOG_ENTRIES.value, suspendLogs)
                    put(ThymeLeafMapKeys.ADMIN_LOG_ENTRIES.value, adminLogs)
                    put(ThymeLeafMapKeys.ADMIN_SITE_STATS.value, siteStats)
                    put(ThymeLeafMapKeys.ADMIN_LOG_PAGE.value, adminLogsPage)
                    put(ThymeLeafMapKeys.ADMIN_SUSPEND_LOG_PAGE.value, adminLogsPageOrder)
                    put(ThymeLeafMapKeys.ADMIN_INVITE_CODE_LIST.value, inviteCodes)

                    if (error != null) {
                        put(ThymeLeafMapKeys.ERROR.value, error)
                    }

                    if (success != null) {
                        put(ThymeLeafMapKeys.SUCCESS.value, success)
                    }
                }

                call.respond(ThymeleafContent("admin_panel", map))
            }

            post("/admin/motd/set") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    logger.warn { "User ${getUserName(user)} is not a valid admin user and is attempting to access protected material!" }
                    return@post call.respond(HttpStatusCode.NotFound)
                }
                val params = call.receive<Parameters>()
                val clear = params["clear"]?.toBoolean() ?: false

                if (clear == true) {
                    val ret = setMotd(userId = user, "")

                    if (!ret) {
                        val error = "An error occurred while clearing motd message"
                        return@post call.respondRedirect("/admin?error=$error")
                    }
                    return@post call.respondRedirect("/admin?success=Cleared motd message")
                }

                val motd = params["motd"] ?: return@post call.respond(HttpStatusCode.BadRequest)

                //Again arbitrary check just to ensure you cant put insanelty large strings in, no useful advice because someone doing this isnt doing it in good faith most likely
                if (motd.length > 600) {
                    val error = "MOTD is too long"
                    return@post call.respondRedirect("/admin?error=$error")
                }
                val ret = setMotd(userId = user, motd)

                if (!ret) {
                    val error = "An error occurred while setting motd message"
                    return@post call.respondRedirect("/admin?error=$error")
                }

                val success = "Successfully set motd message"
                return@post call.respondRedirect("/admin?success=$success")

            }

            post("/admin/infomessage/set") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    logger.warn { "User ${getUserName(user)} is not a valid admin user and is attempting to access protected material!" }
                    return@post call.respond(HttpStatusCode.NotFound)
                }
                val params = call.receive<Parameters>()
                val clear = params["clear"]?.toBoolean() ?: false

                if (clear == true) {
                    val ret = setInfoMessage(userId = user, "")

                    if (!ret) {
                        val error = "An error occurred while clearing info message"
                        return@post call.respondRedirect("/admin?error=$error")
                    }
                    return@post call.respondRedirect("/admin?success=Cleared info message")
                }
                val infoMessage = params["message"] ?: ""

                //These values are arbitrary i wont bother putting enums for these. Its just to make sure a staff member doesnt fuck around by trying to insert infinitely long strings into the database or config
                if (infoMessage.length > 600) {
                    val error = "MOTD is too long"
                    return@post call.respondRedirect("/admin?error=$error")
                }


                val ret = setInfoMessage(userId = user, infoMessage)

                if (!ret) {
                    val error = "An error occurred while setting info message"
                    return@post call.respondRedirect("/admin?error=$error")
                }

                val success = "Successfully set info message"
                return@post call.respondRedirect("/admin?success=$success")

            }

            post("/admin/signups/toggle") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    logger.warn { "User ${getUserName(user)} is not a valid admin user and is attempting to access protected material!" }
                    return@post call.respond(HttpStatusCode.NotFound)
                }

                //Is this user a moderator? Only admins can toggle this
                if (!isUserAdmin(user)) {
                    val error = "Only admins can change the signups enabled status"
                    return@post call.respondRedirect("/admin?error=$error")
                }

                val signupsDisabled = areSignupsSuspended()

                when (signupsDisabled) {
                    true -> {
                        unsuspendSignups()
                    }

                    false -> {
                        suspendSignups()
                    }
                }

                val success = "Successfully ${if (signupsDisabled == true) "enabled" else "disabled"} signups"
                return@post call.respondRedirect("/admin?success=$success")

            }

            post("/admin/inviteonly/toggle") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    logger.warn { "User ${getUserName(user)} is not a valid admin user and is attempting to access protected material!" }
                    return@post call.respond(HttpStatusCode.NotFound)
                }
                if (siteConfig?.inviteOnly == true) {
                    val error =
                        "Invite only is set at the application config level, this cannot be overridden. Talk to the site owner if you wish to change this."
                    return@post call.respondRedirect("/admin?error=$error")
                }

                //Is this user a moderator? Only admins can toggle this
                if (!isUserAdmin(user)) {
                    val error = "Only admins can change the invite only status"
                    return@post call.respondRedirect("/admin?error=$error")
                }
                val inviteOnly = isInviteOnlyEnabled()

                when (inviteOnly) {
                    true -> {
                        if (!disableInviteOnly()) {
                            val error = "An error occurred while disabling invite only"
                            return@post call.respondRedirect("/admin?error=$error")
                        }
                        val success = "Successfully disabled invite only"
                        return@post call.respondRedirect("/admin?success=$success")
                    }

                    false -> {
                        if (!setInviteOnlyEnabled()) {
                            val error = "An error occurred while enabling invite only"
                            return@post call.respondRedirect("/admin?error=$error")
                        }
                        val success = "Successfully enabled invite only"
                        return@post call.respondRedirect("/admin?success=$success")
                    }

                    null -> {

                        val error =
                            "An error that should never happen has occurred. Site config null or database access error. Please report this to the owner."
                        return@post call.respondRedirect("/admin?error=$error")

                    }
                }

            }

            post("/admin/invites/generate") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    logger.warn { "User ${getUserName(user)} is not a valid admin user and is attempting to access protected material!" }
                    return@post call.respond(HttpStatusCode.NotFound)
                }

                val ret = generateNewInviteCode(user)

                if (ret == null) {
                    val error = "An error occurred while generating invite code"
                    return@post call.respondRedirect("/admin?error=$error")
                }

                if (ret == RetStrings.MAX_REACHED.value) {
                    val error =
                        "You have the reached the max number of unused invite codes allowed (${Length.MAX_INVITE_CODES.value})"
                    return@post call.respondRedirect("/admin?error=$error")
                }

                val success = "Generated invite code"
                call.respondRedirect("/admin?success=$success")

            }

            post("/admin/give") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    logger.warn { "User ${getUserName(user)} is not a valid admin user and is attempting to access protected material!" }
                    return@post call.respond(HttpStatusCode.NotFound)
                }

                //Is this user a moderator? Only admins can toggle this
                if (!isUserAdmin(user)) {
                    val error = "Only admins can give or take admin status"
                    return@post call.respondRedirect("/admin?error=$error")
                }
                val params = call.receiveParameters()

                val username = params["username"] ?: return@post call.respond(HttpStatusCode.BadRequest)

                if (username.length > 100) {
                    val error = "Invalid parameter lengths"
                    return@post call.respondRedirect("/admin?error=$error")
                }
                val userId = getUserId(username) ?: return@post call.respondRedirect("/admin?error=$username not found")

                val ret = giveAdmin(userId, user)

                if (!ret) {
                    val error = "An error occurred while giving ${username} admin"
                    return@post call.respondRedirect("/admin?error=$error")
                }

                val success = "Successfully gave $username admin"
                return@post call.respondRedirect("/admin?success=$success")
            }
            post("/admin/take") {

                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()


                if (!isUserAdminOrModerator(user!!)) {
                    logger.warn { "User ${getUserName(user)} is not a valid admin user and is attempting to access protected material!" }
                    return@post call.respond(HttpStatusCode.NotFound)
                }

                //Is this user a moderator? Only admins can toggle this
                if (!isUserAdmin(user)) {
                    val error = "Only admins can give or take admin status"
                    return@post call.respondRedirect("/admin?error=$error")
                }

                val params = call.receiveParameters()
                val username = params["username"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val reason = params["reason"] ?: return@post call.respond(HttpStatusCode.BadRequest)

                if (reason.length > 300 || username.length > 100) {
                    val error = "Invalid parameter lengths"
                    return@post call.respondRedirect("/admin?error=$error")
                }

                val userId = getUserId(username) ?: return@post call.respondRedirect("/admin?error=$username not found")

                val ret = takeAdmin(userId, user, reason)

                if (!ret) {
                    val error = "An error occurred while giving ${username} admin"
                    return@post call.respondRedirect("/admin?error=$error")
                }

                val success = "Successfully gave $username admin"
                return@post call.respondRedirect("/admin?success=$success")
            }

            post("/moderator/give") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    logger.warn { "User ${getUserName(user)} is not a valid admin user and is attempting to access protected material!" }
                    return@post call.respond(HttpStatusCode.NotFound)
                }
                //Is this user a moderator? Only admins can toggle this
                if (!isUserAdmin(user)) {
                    val error = "Only admins can give or take admin status"
                    return@post call.respondRedirect("/admin?error=$error")
                }
                val params = call.receiveParameters()
                val username = params["username"] ?: return@post call.respond(HttpStatusCode.BadRequest)

                if (username.length > 100) {
                    val error = "Invalid parameter lengths"
                    return@post call.respondRedirect("/admin?error=$error")
                }
                val userId = getUserId(username) ?: return@post call.respondRedirect("/admin?error=$username not found")


                val ret = giveAdmin(userId, user)

                if (!ret) {
                    val error = "An error occurred while giving ${username} moderator"
                    return@post call.respondRedirect("/admin?error=$error")
                }

                val success = "Successfully gave $username moderator"
                return@post call.respondRedirect("/admin?success=$success")
            }
            post("/moderator/take") {

                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()


                if (!isUserAdminOrModerator(user!!)) {
                    logger.warn { "User ${getUserName(user)} is not a valid admin user and is attempting to access protected material!" }
                    return@post call.respond(HttpStatusCode.NotFound)
                }

                //Is this user a moderator? Only admins can toggle this
                if (!isUserAdmin(user)) {
                    val error = "Only admins can give or take admin status"
                    return@post call.respondRedirect("/admin?error=$error")
                }

                val params = call.receiveParameters()

                val username = params["username"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val reason = params["reason"] ?: return@post call.respond(HttpStatusCode.BadRequest)

                if (reason.length > 300 || username.length > 100) {
                    val error = "Invalid parameter lengths"
                    return@post call.respondRedirect("/admin?error=$error")
                }

                val userId = getUserId(username) ?: return@post call.respondRedirect("/admin?error=$username not found")

                val ret = takeAdmin(userId, user, reason)

                if (!ret) {
                    val error = "An error occurred while giving ${username} moderator"
                    return@post call.respondRedirect("/admin?error=$error")
                }

                val success = "Successfully gave $username moderator"
                return@post call.respondRedirect("/admin?success=$success")
            }

            post("/admin/suspend") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    logger.warn { "User ${getUserName(user)} is not a valid admin user and is attempting to access protected material!" }
                    return@post call.respond(HttpStatusCode.NotFound)
                }
                val params = call.receiveParameters()
                val username = params["username"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val reason = params["reason"] ?: return@post call.respond(HttpStatusCode.BadRequest)


                if (reason.length > 300 || username.length > 100) {
                    val error = "Invalid parameter lengths"
                    return@post call.respondRedirect("/admin?error=$error")
                }
                val userId = getUserId(username) ?: return@post call.respondRedirect("/admin?error=$username not found")

                if (isUserAdminOrModerator(userId)) {
                    val error = "You cannot suspend an admin or moderator while they still have active status"
                    return@post call.respondRedirect("/admin?error=$error")
                }


                val ret = suspendUser(userId, user, reason)

                if (!ret) {
                    val error = "An error occurred while suspending ${username}"
                    return@post call.respondRedirect("/admin?error=$error")
                }

                val success = "Successfully suspended user ${username}"
                return@post call.respondRedirect("/admin?success=$success")


            }

            post("/admin/unsuspend") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    logger.warn { "User ${getUserName(user)} is not a valid admin user and is attempting to access protected material!" }
                    return@post call.respond(HttpStatusCode.NotFound)
                }
                val params = call.receiveParameters()
                val username = params["username"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val reason = params["reason"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                if (reason.length > 300 || username.length > 100) {
                    val error = "Invalid parameter lengths"
                    return@post call.respondRedirect("/admin?error=$error")
                }
                val userId = getUserId(username) ?: return@post call.respondRedirect("/admin?error=$username not found")

                if (isUserAdminOrModerator(userId)) {
                    val error = "You cannot suspend/unsuspend an admin or moderator while they still have active status"
                    return@post call.respondRedirect("/admin?error=$error")
                }


                val ret = unSuspendUser(userId, user, reason)

                if (!ret) {
                    val error = "An error occurred while suspending ${username}"
                    return@post call.respondRedirect("/admin?error=$error")
                }

                val success = "Successfully unsuspended user ${username}"
                return@post call.respondRedirect("/admin?success=$success")
            }


        }


    }


}