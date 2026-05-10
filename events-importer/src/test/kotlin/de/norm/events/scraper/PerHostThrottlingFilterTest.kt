package de.norm.events.scraper

import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import java.net.URI
import kotlin.system.measureTimeMillis

/**
 * Unit tests for [PerHostThrottlingFilter].
 *
 * Verifies the per-host politeness delay logic: same-host requests are
 * throttled while different-host requests proceed independently.
 * Uses a mocked [ExchangeFunction] — no real HTTP calls are made.
 */
class PerHostThrottlingFilterTest {
    /** Short delay for fast tests while still being measurable. */
    private val politeDelayMillis = 200L

    private val filter = PerHostThrottlingFilter(politeDelayMillis)

    private val mockResponse: ClientResponse = mockk()
    private val mockExchange: ExchangeFunction =
        mockk {
            every { exchange(any()) } returns Mono.just(mockResponse)
        }

    /** Creates a minimal [ClientRequest] targeting the given [url]. */
    private fun request(url: String): ClientRequest = ClientRequest.create(org.springframework.http.HttpMethod.GET, URI.create(url)).build()

    @Nested
    inner class SameHostThrottling {
        @Test
        fun `first request to a host is not delayed`() =
            runTest {
                val elapsed =
                    measureTimeMillis {
                        filter.filter(request("https://example.com/page1"), mockExchange).block()
                    }

                // First request should complete without any throttle delay
                elapsed shouldBeLessThan politeDelayMillis
                verify(exactly = 1) { mockExchange.exchange(any()) }
            }

        @Test
        fun `consecutive requests to the same host are delayed`() =
            runTest {
                // First request — no delay
                filter.filter(request("https://example.com/page1"), mockExchange).block()

                // Second request — should be throttled by at least politeDelayMillis
                val elapsed =
                    measureTimeMillis {
                        filter.filter(request("https://example.com/page2"), mockExchange).block()
                    }

                elapsed shouldBeGreaterThanOrEqual politeDelayMillis
                verify(exactly = 2) { mockExchange.exchange(any()) }
            }

        @Test
        fun `third request is also delayed relative to the second`() =
            runTest {
                filter.filter(request("https://example.com/a"), mockExchange).block()
                filter.filter(request("https://example.com/b"), mockExchange).block()

                val elapsed =
                    measureTimeMillis {
                        filter.filter(request("https://example.com/c"), mockExchange).block()
                    }

                elapsed shouldBeGreaterThanOrEqual politeDelayMillis
            }
    }

    @Nested
    inner class DifferentHostConcurrency {
        @Test
        fun `requests to different hosts are not delayed by each other`() =
            runTest {
                // First request to host A
                filter.filter(request("https://host-a.com/page"), mockExchange).block()

                // First request to host B — should NOT wait for host A's throttle
                val elapsed =
                    measureTimeMillis {
                        filter.filter(request("https://host-b.com/page"), mockExchange).block()
                    }

                elapsed shouldBeLessThan politeDelayMillis
                verify(exactly = 2) { mockExchange.exchange(any()) }
            }

        @Test
        fun `each host maintains its own throttle independently`() =
            runTest {
                // Warm up both hosts
                filter.filter(request("https://host-a.com/1"), mockExchange).block()
                filter.filter(request("https://host-b.com/1"), mockExchange).block()

                // Second request to host A — should be delayed (same host)
                val elapsedA =
                    measureTimeMillis {
                        filter.filter(request("https://host-a.com/2"), mockExchange).block()
                    }

                elapsedA shouldBeGreaterThanOrEqual politeDelayMillis
            }
    }

    @Nested
    inner class ConcurrentSameHostRequests {
        @Test
        fun `concurrent requests to the same host are serialized`() =
            runTest {
                // Fire two requests concurrently to the same host after a warm-up
                filter.filter(request("https://example.com/warmup"), mockExchange).block()

                val elapsed =
                    measureTimeMillis {
                        val a = async { filter.filter(request("https://example.com/a"), mockExchange).block() }
                        val b = async { filter.filter(request("https://example.com/b"), mockExchange).block() }
                        a.await()
                        b.await()
                    }

                // Both should have been serialized: at least 2x delay (warmup→a, a→b)
                elapsed shouldBeGreaterThanOrEqual (politeDelayMillis * 2)
                verify(exactly = 3) { mockExchange.exchange(any()) }
            }
    }

    @Nested
    inner class ZeroDelay {
        @Test
        fun `zero delay allows consecutive requests without throttling`() =
            runTest {
                val noDelayFilter = PerHostThrottlingFilter(politeDelayMillis = 0)

                noDelayFilter.filter(request("https://example.com/1"), mockExchange).block()

                val elapsed =
                    measureTimeMillis {
                        noDelayFilter.filter(request("https://example.com/2"), mockExchange).block()
                    }

                // With 0ms delay, the second request should not be delayed significantly
                elapsed shouldBeLessThan 50L
            }
    }

    @Nested
    inner class DelegationToNext {
        @Test
        fun `filter delegates to the next exchange function and returns its response`() =
            runTest {
                val result = filter.filter(request("https://example.com/page"), mockExchange).block()

                result shouldBe mockResponse
                verify(exactly = 1) { mockExchange.exchange(any()) }
            }
    }
}
