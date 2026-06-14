package cantstopthesignal.database.admin

import cantstopthesignal.database.users.getUserName
import cantstopthesignal.database.users.isUserAdminOrModerator
import com.freedom.cantstopthesignal.siteConfig

fun getMotd(): String? {
    return if (siteConfig?.motd == null) null else siteConfig?.motd
}


fun getInfoMessage(): String? {
    return if (siteConfig?.motd == null) null else siteConfig?.motd
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
    return if (siteConfig?.motd == null) null else siteConfig?.motd
}

fun setInviteOnly(): Boolean? {
    if (siteConfig?.inviteOnly == null) {
        return null
    }
    siteConfig?.inviteOnly = true
    return true
}


