package de.norm.events

import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.config.MeterFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Tunes the auto-configured HTTP client metrics for the scraper workload.
 *
 * The importer fetches an unbounded set of external venue URLs through the
 * instrumented [org.springframework.web.reactive.function.client.WebClient], and
 * each distinct URL becomes its own value of the `uri` tag on the
 * `http.client.requests` meter. That cardinality is unbounded and useless as a
 * metric dimension (one venue import alone produces dozens of one-off URLs), and
 * left unchecked it trips Micrometer's default 100-tag cap — logged as the
 * `MaximumAllowableTagsMeterFilter` "Reached the maximum number of 'uri' tags"
 * warning — and would grow the registry's memory in production.
 *
 * This filter drops the `uri` tag from `http.client.requests` only. It leaves
 * every other meter — notably `http.server.requests`, whose `uri` is the
 * low-cardinality templated route (e.g. `/api/admin/venues/{id}`) and is worth
 * keeping — untouched.
 */
@Configuration
class MetricsConfiguration {
    @Bean
    fun ignoreHttpClientUriTag(): MeterFilter =
        object : MeterFilter {
            override fun map(id: Meter.Id): Meter.Id =
                if (id.name == HTTP_CLIENT_REQUESTS) {
                    id.replaceTags(Tags.of(id.tags.filterNot { it.key == URI_TAG }))
                } else {
                    id
                }
        }

    private companion object {
        const val HTTP_CLIENT_REQUESTS = "http.client.requests"
        const val URI_TAG = "uri"
    }
}
