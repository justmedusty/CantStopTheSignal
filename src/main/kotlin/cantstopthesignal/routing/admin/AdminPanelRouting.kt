package com.freedom.cantstopthesignal.routing.admin

import cantstopthesignal.database.admin.*
import cantstopthesignal.database.invite_only.generateNewInviteCode
import cantstopthesignal.database.invite_only.getAllValidLoginCodes
import cantstopthesignal.database.users.getUserName
import cantstopthesignal.database.users.isUserAdminOrModerator
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.admin.getSiteStats
import com.freedom.cantstopthesignal.database.sitewide_permissions.areSignupsSuspended
import com.freedom.cantstopthesignal.enums.Length
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
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
                val inviteOnly = getInviteOnly()

                val inviteCodes: List<String>? = getAllValidLoginCodes(
                    user,
                    1,
                    100
                )//Im just gonna show 100, you should never need to page through these so don't think I'll implement it. If you need to give out an invite code, why would you page through them? Just pick one.
                logger.info { inviteCodes }

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

                    if(error != null) {
                        put(ThymeLeafMapKeys.ERROR.value, error)
                    }

                    if(success != null){
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

                val motd = params["motd"] ?: return@post call.respond(HttpStatusCode.BadRequest)

            }

            post("/admin/info/set") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    logger.warn { "User ${getUserName(user)} is not a valid admin user and is attempting to access protected material!" }
                    return@post call.respond(HttpStatusCode.NotFound)
                }
                val params = call.receive<Parameters>()
                val infoMessage = params["infoMessage"] ?: return@post call.respond(HttpStatusCode.BadRequest)


            }

            post("/admin/infomessage/update") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    logger.warn { "User ${getUserName(user)} is not a valid admin user and is attempting to access protected material!" }
                    return@post call.respond(HttpStatusCode.NotFound)
                }

            }
            post("/admin/motd/update") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    logger.warn { "User ${getUserName(user)} is not a valid admin user and is attempting to access protected material!" }
                    return@post call.respond(HttpStatusCode.NotFound)
                }
            }

            post("/admin/signups/toggle") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    logger.warn { "User ${getUserName(user)} is not a valid admin user and is attempting to access protected material!" }
                    return@post call.respond(HttpStatusCode.NotFound)
                }

            }

            post("/admin/inviteonly/toggle") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    logger.warn { "User ${getUserName(user)} is not a valid admin user and is attempting to access protected material!" }
                    return@post call.respond(HttpStatusCode.NotFound)
                }

            }

            post("/admin/invites/generate") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    logger.warn { "User ${getUserName(user)} is not a valid admin user and is attempting to access protected material!" }
                    return@post call.respond(HttpStatusCode.NotFound)
                }

                generateNewInviteCode(user)
                val success = "Generated invite code"
                call.respondRedirect("/admin?success=$success")

            }


        }


    }


}