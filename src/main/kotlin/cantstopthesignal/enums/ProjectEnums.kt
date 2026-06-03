package com.freedom.cantstopthesignal.enums

enum class Length(val value: Long) {
    MAX_CONTENT_LENGTH(20_000),
    MAX_TITLE_LENGTH(300),
    MAX_COMMENT_LENGTH(10_000),
    MAX_TOPIC_LENGTH(40),
    MIN_USERNAME_LENGTH(3),
    MAX_USERNAME_LENGTH(20),
    MIN_PASSWORD_LENGTH(8),
    MAX_PASSWORD_LENGTH(75),
    MAX_PHOTO_SIZE_BYTES(1_048_576),
    MAX_DM_MESSAGE_LENGTH(5_000),
    MAX_PAGE_LIMIT(50),
    MAX_GROUPNAME_LENGTH(100),
    MAX_MEMBERS_IN_CONVERSATION(15),
    JWT_TOKEN_LIFETIME_MS(1000 * 60 * 20) //20 minutes, this mostly serves as a backup ins case the application.yaml value is taken out for some reason, don't do that. But I prepared just in case you do.
}

enum class Notif(val value: Long) {
    POST_COMMENT(1),
    COMMENT_REPLY(2),
    COMMENT_LIKE(3),
    POST_LIKE(4),
}

enum class ThymeLeafMapKeys(val value: String) {
    SUCCESS("success"),
    ERROR("error"),
    POSTS("posts"),
    COMMENTS("comments"),

    /* Since I am going to have two different templates, one for top level comments and one for a focal comment with comment replies, these two are separate from the comments key above*/
    COMMENT_BEING_REPLIED_TO("focal_comment"),
    COMMENT_REPLIES("replies"),
    PROFILE_DATA("profile_data"),
    MESSAGE_NOTIFICATIONS("message_notifications"),
    OTHER_NOTIFICATIONS("other_notifications"),
    EDIT_INFORMATION("edit_information"),
    ADMIN_LOG("admin_log"),
    POST_LIKES("post_likes"),
    POST_DISLIKES("post_dislikes"),
    PRIVATE_MESSAGE_DRAFT("draft_message"),
    PRIVATE_MESSAGE_CONVERSATION_DRAFT("conversation_draft"),
    PRIVATE_MESSAGE_LIST("private_message_list"),
    PRIVATE_MESSAGE_CONVERSATION("message_conversations"),
    USER_COMMENT_HISTORY("user_comment_history"),
    USER_POST_HISTORY("user_post_history"),
    SUSPEND_LOGS("suspension_logs"),
    SERVER_CONFIG("server_config"), //This one will be for pluggable values to show users such as an MOTD or a different name in case someone wishes to use my code for their own forum website
    CURRENT_PAGE("current_page"),
    CURRENT_LIMIT("current_limit"),
    TOTAL_PAGES("total_pages"),
}

enum class SortOrderValues(val value: String) {
    NEWEST("newest"),
    OLDEST("oldest"),
    LIKES("likes"),
    DISLIKES("dislikes"),
}


enum class RetValues(val value: Long) {
    ALREADY_EXISTS(-50)
}

enum class RegexPatterns(val value: Regex) {
    USERNAME("""^[a-zA-Z0-9_]+""".toRegex()),
}