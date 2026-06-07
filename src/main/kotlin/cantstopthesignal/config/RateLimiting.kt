package cantstopthesignal.config

import com.freedom.cantstopthesignal.siteConfig
import io.github.flaxoos.ktor.server.plugins.ratelimiter.RateLimiting
import io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations.TokenBucket
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureRateLimiting() {
    routing {
        route("/") {
            install(RateLimiting) {
                rateLimiter {
                    type = TokenBucket::class
                    capacity = siteConfig?.rateLimitNumAllowedInWindow?.toInt() ?: 100
                    rate = siteConfig?.rateLimitWindowSeconds?.toInt()?.seconds ?: 10.seconds
                }
            }
        }
        route("/login") {
            install(RateLimiting) {
                rateLimiter {
                    type = TokenBucket::class
                    capacity = siteConfig?.rateLimitNumAllowedInWindowLoginSignup?.toInt() ?: 100
                    rate = siteConfig?.rateLimitWindowSecondsLoginSignup?.toInt()?.seconds ?: 10.seconds
                }
            }
        }
        route("/signup") {
            install(RateLimiting) {
                rateLimiter {
                    type = TokenBucket::class /* I assume you can use something other than the token bucket algorithim, maybe something to check out */
                    capacity = siteConfig?.rateLimitNumAllowedInWindowLoginSignup?.toInt() ?: 100
                    rate = siteConfig?.rateLimitWindowSecondsLoginSignup?.toInt()?.seconds ?: 10.seconds
                }
            }
        }
    }
}
