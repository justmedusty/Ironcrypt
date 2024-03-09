package com.ironcrypt.configuration
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.ApplicationCallPipeline.ApplicationPhase.Plugins
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class RateLimitConfig(var lastRequestTime: Long, var requestCount: Int)

val rateLimitMap = ConcurrentHashMap<String, RateLimitConfig>()

fun Application.configureRateLimiting() {
    intercept(Plugins) {
        val ip = call.request.origin.remoteHost
        val currentTime = System.currentTimeMillis()
        val rateLimitInfo = rateLimitMap.getOrPut(ip) { RateLimitConfig(currentTime, 0) }

        if (call.request.uri == "/ironcrypt/login" || call.request.uri == "/ironcrypt/signup") {
            if (currentTime - rateLimitInfo.lastRequestTime > 1.minutes.inWholeMilliseconds) {
                rateLimitInfo.requestCount = 1
                rateLimitInfo.lastRequestTime = currentTime
            } else {
                rateLimitInfo.requestCount++
                if (rateLimitInfo.requestCount > 15) {
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        mapOf("Response" to "Too many requests, rate limit exceeded"),
                    )
                    finish()
                }
            }

        } else {
            if (currentTime - rateLimitInfo.lastRequestTime > 5.seconds.inWholeMilliseconds) {
                rateLimitInfo.requestCount = 1
                rateLimitInfo.lastRequestTime = currentTime
            } else {
                rateLimitInfo.requestCount++
                if (rateLimitInfo.requestCount > 15) {
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        mapOf("Response" to "Too many requests, rate limit exceeded"),
                    )
                    finish()
                }
            }
        }
    }
}