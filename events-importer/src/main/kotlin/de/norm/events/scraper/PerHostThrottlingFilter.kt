package de.norm.events.scraper

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * WebClient [ExchangeFilterFunction] that enforces a politeness delay between
 * consecutive HTTP requests to the **same** host.
 *
 * Each host gets its own [Mutex] and monotonic timestamp so that requests to
 * different venues proceed concurrently, while requests to the same server are
 * spaced at least [politeDelayMillis] apart. This prevents overwhelming target
 * servers during web scraping — the throttling is transparent to callers and
 * applies automatically to every request made through the configured WebClient.
 *
 * Uses [kotlinx.coroutines.reactor.mono] to bridge between the reactive
 * [ExchangeFilterFunction] contract and coroutine-based [Mutex]/[delay].
 *
 * @param politeDelayMillis minimum time (in milliseconds) between consecutive
 *   requests to the same host. Requests arriving sooner will suspend until the
 *   delay has elapsed.
 */
class PerHostThrottlingFilter(
    private val politeDelayMillis: Long
) : ExchangeFilterFunction {
    private val logger = KotlinLogging.logger {}

    /**
     * Per-host throttle state. Entries are created lazily on first access and
     * kept for the application lifetime (the set of target hosts is small and
     * bounded by the number of configured venues).
     */
    private val hostThrottles = ConcurrentHashMap<String, HostThrottle>()

    override fun filter(
        request: ClientRequest,
        next: ExchangeFunction
    ): Mono<ClientResponse> {
        val host = request.url().host ?: return next.exchange(request)

        // Bridge into a coroutine so we can use Mutex + delay, then
        // flatMap into the actual HTTP exchange which stays fully reactive.
        return mono { awaitThrottle(host) }
            .then(next.exchange(request))
    }

    /**
     * Acquires the per-host mutex and suspends if the elapsed time since
     * the last request to [host] is shorter than [politeDelayMillis].
     * Records the current timestamp before releasing the mutex so the
     * next caller sees the correct baseline.
     */
    private suspend fun awaitThrottle(host: String) {
        val throttle = hostThrottles.computeIfAbsent(host) { HostThrottle() }

        throttle.mutex.withLock {
            throttle.lastRequestMark?.let { mark ->
                val remaining = politeDelayMillis.milliseconds - mark.elapsedNow()
                if (remaining.isPositive()) {
                    logger.debug { "Throttling $host: waiting $remaining before next request" }
                    delay(remaining)
                }
            }
            throttle.lastRequestMark = TimeSource.Monotonic.markNow()
        }
    }
}

/**
 * Per-host throttle state holding a [Mutex] to serialize requests and
 * the [TimeSource.Monotonic.ValueTimeMark] of the most recent request.
 */
private class HostThrottle {
    val mutex = Mutex()
    var lastRequestMark: TimeSource.Monotonic.ValueTimeMark? = null
}
