package cantstopthesignal.config

import com.freedom.cantstopthesignal.siteConfig
import io.ktor.server.application.Application

data class SiteConfig(
    val name: String,
    val topic: String,
    val motd: String,
    val issuer: String,
    val baseUrl: String,

)

fun Application.loadSiteConfig() {
    val config = environment.config.config("site")
    siteConfig = SiteConfig(
        name = config.property("name").getString(),
        topic = config.property("topic").getString(),
        motd = config.property("motd").getString(),
        issuer = config.property("issuer").getString(),
        baseUrl = config.property("baseUrl").getString(),
    )
}