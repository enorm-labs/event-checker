package de.norm.events.scraper

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Configuration properties for the HTML scraping infrastructure.
 *
 * Bound to the `app.scraper` prefix in `application.yaml`. Controls timeouts
 * and rate limiting for outbound HTTP requests made by [HtmlFetcher].
 *
 * The response body size limit is configured separately via the standard
 * Spring Boot property `spring.http.codecs.max-in-memory-size` (applies globally
 * to all WebClient codecs).
 */
@ConfigurationProperties(prefix = "app.scraper")
data class ScraperProperties(
    /** Maximum time to wait for a server response before aborting the fetch. */
    val responseTimeout: Duration = Duration.ofSeconds(DEFAULT_RESPONSE_TIMEOUT_SECONDS),
    /** Politeness delay between consecutive detail page fetches to avoid overwhelming target servers. */
    val politeDelayMillis: Long = DEFAULT_POLITE_DELAY_MILLIS
) {
    companion object {
        private const val DEFAULT_RESPONSE_TIMEOUT_SECONDS = 30L
        private const val DEFAULT_POLITE_DELAY_MILLIS = 200L
    }
}
