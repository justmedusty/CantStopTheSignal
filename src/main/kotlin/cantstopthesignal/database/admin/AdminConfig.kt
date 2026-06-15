package cantstopthesignal.database.admin

import cantstopthesignal.database.users.getUserName
import cantstopthesignal.database.users.isUserAdminOrModerator
import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.siteConfig

fun getMotd(): String? {
    return if (siteConfig?.motd == null) null else siteConfig?.motd
}


fun getInfoMessage(): String? {
    return if (siteConfig?.infoMessage == null) null else siteConfig?.infoMessage
}


fun setMotd(userId: Long, newMotd: String): Boolean {
    if (!isUserAdminOrModerator(userId)) return false
    if (newMotd == siteConfig?.motd) return true // bit redundant but thats okay
    insertAdminLogEntry(
        userId,
        "Changing the MOTD",
        "${getUserName(userId)} changed the motd to ${newMotd}"
    )
    siteConfig?.motd = newMotd
    return true
}

fun setInfoMessage(userId: Long, newMessage: String): Boolean {
    if (!isUserAdminOrModerator(userId)) return false

    insertAdminLogEntry(
        userId,
        "Changing the info message",
        "${getUserName(userId)} changed the info message to ${newMessage}"
    )

    siteConfig?.infoMessage = newMessage

    return true
}

fun getInviteOnly(): String? {
    return if (siteConfig?.inviteOnly == null) null else siteConfig?.inviteOnly.toString()
}

fun setInviteOnly(on : Boolean): Boolean? {
    /*
        This should never happen but we'll check anyway
     */
    if (siteConfig?.inviteOnly == null) {
        logger.error { "site config is null, this should never happen!" }
        return null
    }

    if(siteConfig?.inviteOnly == true) {
        logger.warn { "An admin is trying to override a setting that , when set, cannot be overridden! invite only" }
        return null
    }

    return true
}


