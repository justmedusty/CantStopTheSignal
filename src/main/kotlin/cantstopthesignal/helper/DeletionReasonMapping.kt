package cantstopthesignal.helper

import cantstopthesignal.enums.DeletionReason

fun getDeletionReasonString(reasonId: Long?): String {
    return when (reasonId) {
        DeletionReason.DELETED_BY_USER.value -> {
            "Deleted by user"
        }

        DeletionReason.ADMIN_SPAM.value -> {
            "Deleted by admins for reason: spam"
        }

        DeletionReason.ADMIN_BAD_FAITH.value -> {
            "Deleted by admins for reason: bad faith"
        }

        DeletionReason.ADMIN_SENSITIVE_INFO.value -> {
            "Deleted by admins for reason: sensitive/identifying info"
        }

        else -> {
            "Deleted for unknown reason by unknown party"
        }
    }
}