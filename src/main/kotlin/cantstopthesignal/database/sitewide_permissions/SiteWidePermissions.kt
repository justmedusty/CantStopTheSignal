package com.freedom.cantstopthesignal.database.sitewide_permissions

import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.enums.SiteWidePermissions
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.ZoneOffset
import com.freedom.cantstopthesignal.database.dsl.table_definitions.SiteWidePermissions as SiteWidePermissionsDb

/*
    This is for emergency admin actions actions
    This will be for an admin only interface
 */

fun areSignupsSuspended(): Boolean {
    return try {
        transaction {
            SiteWidePermissionsDb.selectAll()
                .where { SiteWidePermissionsDb.eventId eq SiteWidePermissions.SUSPENDED_SIGNUPS.value.toLong() }.count()
                .toInt() == 1
        }
    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to check if signups were suspended" }
        true
    }

}

/* This will be delegated to admins only, visible in the admin panel */
fun suspendSignups(callingUser: Long): Boolean {
    return try {
        if (areSignupsSuspended()) {
            return false
        }
        transaction {

            val id = SiteWidePermissionsDb.insert {
                it[SiteWidePermissionsDb.timestamp] = java.time.LocalDateTime.now(ZoneOffset.UTC)
                it[eventId] = SiteWidePermissions.SUSPENDED_SIGNUPS.value.toLong()
            }[SiteWidePermissionsDb.id]

            id != null
        }
    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to suspend signups" }
        false
    }
}

