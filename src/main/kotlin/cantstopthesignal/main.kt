package com.freedom.cantstopthesignal

import cantstopthesignal.config.SiteConfig
import io.ktor.server.netty.EngineMain
var siteConfig : SiteConfig? = null // This will store configurable values for site customization
fun main(args: Array<String>) {
    EngineMain.main(args)
}
