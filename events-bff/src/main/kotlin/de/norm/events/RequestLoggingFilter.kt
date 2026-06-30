package de.norm.events

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

/**
 * Emits a single INFO access-log line per request once the exchange completes:
 * `GET /venues?q=astra -> 200 (12ms)`. WebFlux does not log requests at INFO by
 * default, so without this the read API is effectively silent in the logs.
 *
 * Registered with [Ordered.HIGHEST_PRECEDENCE] so it wraps the whole filter chain
 * and the measured duration reflects total in-server time.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestLoggingFilter : WebFilter {
    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain
    ): Mono<Void> {
        val request = exchange.request
        val startNanos = System.nanoTime()
        return chain.filter(exchange).doFinally {
            val durationMs = (System.nanoTime() - startNanos) / 1_000_000
            val query = request.uri.rawQuery?.let { "?$it" } ?: ""
            val status = exchange.response.statusCode?.value() ?: 0
            logger.info { "${request.method} ${request.path.value()}$query -> $status (${durationMs}ms)" }
        }
    }
}
