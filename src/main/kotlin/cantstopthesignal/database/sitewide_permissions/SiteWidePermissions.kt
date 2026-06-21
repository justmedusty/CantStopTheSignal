package cantstopthesignal.database.sitewide_permissions

import cantstopthesignal.log.logger
import cantstopthesignal.enums.SiteWidePermissions
import cantstopthesignal.siteConfig
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.ZoneOffset
import cantstopthesignal.table_definitions.SiteWidePermissions as SiteWidePermissionsDb

/*
    This is for emergency admin actions actions
    This will be for an admin only interface
 */

fun areSignupsSuspended(): Boolean {
    return try {
        /*
            The site config will supercede a database entry
         */
        if (siteConfig?.signupsDisabled == true) {
            return true
        }
        transaction {
            val ret = SiteWidePermissionsDb.selectAll()
                .where { SiteWidePermissionsDb.eventId eq SiteWidePermissions.SUSPENDED_SIGNUPS.value.toLong() }
                .count() > 0
            logger.debug { "Site wide permissions suspended: $ret" }
            ret //Because the way the thymeleaf template is rigged up we need to negate it
        }
    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to check if signups were suspended" }
        true
    }

}

/* This will be delegated to admins only, visible in the admin panel */
fun suspendSignups(): Boolean {
    return try {
        if (areSignupsSuspended()) {
            return false
        }
        transaction {

            val id = SiteWidePermissionsDb.insert {
                it[SiteWidePermissionsDb.timestamp] = java.time.LocalDateTime.now(ZoneOffset.UTC)
                it[eventId] = SiteWidePermissions.SUSPENDED_SIGNUPS.value.toLong()
            }[SiteWidePermissionsDb.id]

            true
        }
    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to suspend signups" }
        false
    }
}

fun unsuspendSignups(): Boolean {
    return try {
        if (!areSignupsSuspended()) {
            return false
        }
        transaction {
            val id =
                SiteWidePermissionsDb.deleteWhere { SiteWidePermissionsDb.eventId eq SiteWidePermissions.SUSPENDED_SIGNUPS.value.toLong() }
            (id > 0)
        }
    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to suspend signups" }
        false
    }
}

fun isInviteOnlyEnabled(): Boolean {
    if (siteConfig?.inviteOnly == true) {
        return true
    }
    return try {
        transaction {
            val ret = SiteWidePermissionsDb.selectAll()
                .where { SiteWidePermissionsDb.eventId eq SiteWidePermissions.INVITE_ONLY.value.toLong() }.count() > 0
            logger.debug { "Site wide invite only $ret" }
            ret
        }
    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to get invite only" }
        false
    }

}

fun setInviteOnlyEnabled(): Boolean {
    if (siteConfig?.inviteOnly == true) {
        //This setting will override the database config option
        return false
    }
    return try {
        transaction {
            val ret = SiteWidePermissionsDb.insert {
                it[eventId] = SiteWidePermissions.INVITE_ONLY.value.toLong()
                it[timestamp] = java.time.LocalDateTime.now(ZoneOffset.UTC)
            }.insertedCount > 0

            logger.debug { "Site wide invite only $ret" }
            ret
        }
    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to enable invite only" }
        false
    }

}

fun disableInviteOnly(): Boolean {
    if (siteConfig?.inviteOnly == true) {
        //This setting will override the database config option
        return false
    }
    return try {
        transaction {
            val ret =
                SiteWidePermissionsDb.deleteWhere { SiteWidePermissionsDb.eventId eq SiteWidePermissions.INVITE_ONLY.value.toLong() } > 0
            logger.debug { "Site wide invite only $ret" }
            ret
        }
    } catch (e: Exception) {
        logger.error { "${e.message} occurred while trying to get invite only" }
        false
    }
}