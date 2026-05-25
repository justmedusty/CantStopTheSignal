package cantstopthesignal.config

import com.freedom.cantstopthesignal.siteConfig
import io.ktor.server.application.Application

data class SiteConfig(
    val name: String,
    val topic: String,
    val motd: String,
    val pgp_help: String,
    val no_encrypt_warning_group: String,
    val no_encrypt_warning_individual: String,
    val encryption_explanation: String,
    val audience: String,
    val issuer: String,
    val baseUrl: String,
    val token_lifetime_minutes: Long

)

fun Application.loadSiteConfig() {
    val config = environment.config.config("site")
    siteConfig = SiteConfig(
        name = config.property("name").getString(),
        topic = config.property("topic").getString(),
        motd = config.property("motd").getString(),
        pgp_help = config.property("pgp_help").getString(),
        no_encrypt_warning_group = config.property("no_encrypt_warning_group").getString(),
        no_encrypt_warning_individual = config.property("no_encrypt_warning_individual").getString(),
        encryption_explanation = config.property("encryption_explanation").getString(),
        audience = config.property("audience").getString(),
        issuer = config.property("issuer").getString(),
        baseUrl = config.property("baseUrl").getString(),
        token_lifetime_minutes = config.property("token_lifetime_minutes").getString().toLong()
    )
}