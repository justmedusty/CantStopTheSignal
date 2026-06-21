package cantstopthesignal.config

import cantstopthesignal.siteConfig
import io.ktor.server.application.*

data class SiteConfig(
    val name: String,
    val topic: String,
    var motd: String,
    var infoMessage: String?,
    val pgpHelp: String,
    val pgpWarning : String,
    val audience: String,
    val issuer: String,
    val baseUrl: String,
    val tokenLifetimeMinutes: Long,
    var inviteOnly: Boolean,
    val rateLimitWindowSeconds: Long,
    val rateLimitNumAllowedInWindow: Long,
    val rateLimitWindowSecondsLoginSignup: Long,
    val rateLimitNumAllowedInWindowLoginSignup: Long,
    val signupsDisabled: Boolean,
    val pgpLoginOnly: Boolean,
    val hoursBetweenMessageDeletionJobs: Long,
)


fun Application.loadSiteConfig() {
    val config = environment.config.config("site")
    siteConfig = SiteConfig(
        name = config.property("name").getString(),
        topic = config.property("topic").getString(),
        motd = config.property("motd").getString(),
        infoMessage = null /* This will be for admins to set in case theres any alerts or announcements*/,
        pgpHelp = config.property("pgp_help").getString(),
        pgpWarning = config.property("pgp_warning").getString(),
        audience = config.property("audience").getString(),
        issuer = config.property("issuer").getString(),
        baseUrl = config.property("baseUrl").getString(),
        tokenLifetimeMinutes = config.property("token_lifetime_minutes").getString().toLong(),
        inviteOnly = config.property("invite_only").getString().toBoolean(),
        rateLimitWindowSeconds = config.property("rate_limit_window_seconds").getString().toLong(),
        rateLimitNumAllowedInWindow = config.property("rate_limit_num_allowed_in_window").getString().toLong(),
        rateLimitWindowSecondsLoginSignup = config.property("rate_limit_window_seconds_login_signup").getString()
            .toLong(),
        rateLimitNumAllowedInWindowLoginSignup = config.property("rate_limit_num_allowed_in_window_login_signup")
            .getString().toLong(),
        signupsDisabled = config.property("signups_disabled").getString().toBoolean(),
        pgpLoginOnly = config.property("pgp_login_only").getString().toBoolean(),
        hoursBetweenMessageDeletionJobs = config.property("message_deletion_window_hours").getString().toLong()
    )
}