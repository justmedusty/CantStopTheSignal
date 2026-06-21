package com.freedom.cantstopthesignal

import cantstopthesignal.config.SiteConfig
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.*


//Our scope for doing cleanup with the PGP challenge entries that cannot exist forever
val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
lateinit var siteConfig: SiteConfig // This will store configurable values for site customization
fun main(args: Array<String>) {
    //Set timezone to UTC since this may be hosted anywhere in the world and we do not want to expose the person hosting the forum to unnecessary data leakage
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    EngineMain.main(args)
}
