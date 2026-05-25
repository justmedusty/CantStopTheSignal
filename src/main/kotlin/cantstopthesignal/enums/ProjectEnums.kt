package com.freedom.cantstopthesignal.enums

enum class Length(val value: Long) {
    MAX_CONTENT_LENGTH(20_000),
    MAX_TITLE_LENGTH(300),
    MAX_COMMENT_LENGTH(10_000),
    MAX_TOPIC_LENGTH(100),
    MAX_PHOTO_SIZE_BYTES(1_048_576),
    MAX_DM_MESSAGE_LENGTH(5_000),
    MAX_PAGE_LIMIT(50),
    JWT_TOKEN_LIFETIME_MS(1000 * 60 * 20) //20 minutes
}

enum class Notif(val value: Long) {
    POST(1),
    COMMENT(2)
}

enum class ThymeLeafMapKeys(val value: String) {
    SUCCESS("success"),
    ERROR("error"),
    POSTS("posts"),
    COMMENTS("comments"),
    PROFILE_DATA("profile_data"),
    MESSAGE_NOTIFICATIONS("message_notifications"),
    OTHER_NOTIFICATIONS("other_notifications"),
    EDIT_INFORMATION("edit_information"),
    ADMIN_LOG("admin_log"),
    POST_LIKES("post_likes"),
    POST_DISLIKES("post_dislikes"),
    PRIVATE_MESSAGE_LIST("private_message_list"),
    PRIVATE_MESSAGE_CONVERSATION("private_message_conversation"),
    USER_COMMENT_HISTORY("user_comment_history"),
    USER_POST_HISTORY("user_post_history"),
    SUSPEND_LOGS("suspension_logs"),
    SERVER_CONFIG("server_config"), //This one will be for pluggable values to show users such as an MOTD or a different name in case someone wishes to use my code for their own forum website
}