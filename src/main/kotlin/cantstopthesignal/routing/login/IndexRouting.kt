package cantstopthesignal.routing.login


import cantstopthesignal.config.SiteConfig
import com.freedom.cantstopthesignal.enums.ThymeLeafMapKeys
import com.freedom.cantstopthesignal.siteConfig
import io.ktor.server.application.Application
import io.ktor.server.application.serverConfig
import io.ktor.server.response.respond
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.ThymeleafContent
import io.ktor.util.AttributeKey

fun Application.indexRouting() {
    routing {

        get("/") {

            val model = buildMap {
                put(
                    ThymeLeafMapKeys.SERVER_CONFIG.value,
                    siteConfig
                )
            }
            call.respond(
                ThymeleafContent("index", model)
            )
        }

    }
}