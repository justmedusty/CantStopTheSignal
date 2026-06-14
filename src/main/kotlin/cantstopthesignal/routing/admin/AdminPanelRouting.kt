package com.freedom.cantstopthesignal.routing.admin

import cantstopthesignal.database.admin.*
import cantstopthesignal.database.users.isUserAdminOrModerator
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
                    return@get call.respond(HttpStatusCode.NotFound)
                }
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
                }

                call.respond(ThymeleafContent("admin_panel", map))
            }

            post("/admin/motd/set") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    return@post call.respond(HttpStatusCode.NotFound)
                }
                val params = call.receive<Parameters>()

                val motd = params["motd"] ?: return@post call.respond(HttpStatusCode.BadRequest)

            }

            post("/admin/info/set") {
                val user = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (!isUserAdminOrModerator(user!!)) {
                    return@post call.respond(HttpStatusCode.NotFound)
                }
                val params = call.receive<Parameters>()
                val infoMessage = params["infoMessage"] ?: return@post call.respond(HttpStatusCode.BadRequest)


            }
        }


    }


}