package cantstopthesignal

import cantstopthesignal.config.SiteConfig
import cantstopthesignal.database.messages.doMessageDeletionRound
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.*
import kotlin.time.Duration.Companion.milliseconds


//Our scope for doing cleanup with the PGP challenge entries that cannot exist forever
val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
lateinit var siteConfig: SiteConfig // This will store configurable values for site customization
fun main(args: Array<String>) {
    //Set timezone to UTC since this may be hosted anywhere in the world and we do not want to expose the person hosting the forum to unnecessary data leakage
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    applicationScope.launch {
        while (true) {
            delay(Duration.ofHours(siteConfig.hoursBetweenMessageDeletionJobs).toMillis().milliseconds)
            //clear message convos marked for auto deletion after hoursBetweenMessageDeletionJobs hours (if all messages are read)
            doMessageDeletionRound()
        }

    }
    EngineMain.main(args)
}
