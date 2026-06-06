package com.freedom.cantstopthesignal

import cantstopthesignal.config.SiteConfig
import io.ktor.server.netty.EngineMain
import java.util.TimeZone

var siteConfig : SiteConfig? = null // This will store configurable values for site customization
fun main(args: Array<String>) {
    //Set timezone to UTC since this may be hosted anywhere in the world and we do not want to expose the person hosting the forum to unnecessary data leakage
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    EngineMain.main(args)
}
