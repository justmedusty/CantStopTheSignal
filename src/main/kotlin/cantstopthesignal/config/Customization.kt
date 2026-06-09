package cantstopthesignal.config

import com.freedom.cantstopthesignal.siteConfig
import io.ktor.server.application.*

data class SiteConfig(
    val name: String,
    val topic: String,
    val motd: String,
    val pgpHelp: String,
    val pgpWarning : String,
    val noEncryptWarningGroup: String,
    val noEncryptWarningIndividual: String,
    val encryptionExplanation: String,
    val audience: String,
    val issuer: String,
    val baseUrl: String,
    val tokenLifetimeMinutes: Long,
    val inviteOnly: Boolean,
    val rateLimitWindowSeconds: Long,
    val rateLimitNumAllowedInWindow: Long,
    val rateLimitWindowSecondsLoginSignup: Long,
    val rateLimitNumAllowedInWindowLoginSignup: Long,
    val signupsDisabled: Boolean,

)

fun Application.loadSiteConfig() {
    val config = environment.config.config("site")
    siteConfig = SiteConfig(
        name = config.property("name").getString(),
        topic = config.property("topic").getString(),
        motd = config.property("motd").getString(),
        pgpHelp = config.property("pgp_help").getString(),
        pgpWarning = config.property("pgp_warning").getString(),
        noEncryptWarningGroup = config.property("no_encrypt_warning_group").getString(),
        noEncryptWarningIndividual = config.property("no_encrypt_warning_individual").getString(),
        encryptionExplanation = config.property("encryption_explanation").getString(),
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
    )
}