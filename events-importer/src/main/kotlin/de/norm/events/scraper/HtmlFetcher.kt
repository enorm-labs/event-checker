package de.norm.events.scraper

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import reactor.netty.http.client.HttpClient
import java.net.URI

/**
 * Reactive HTTP fetcher with conditional-request support and Jsoup parsing.
 *
 * Uses Spring [WebClient] for non-blocking HTTP fetching and Jsoup for
 * HTML parsing. Supports ETag / Last-Modified conditional headers to
 * skip re-downloading pages that haven't changed since the last import.
 *
 * Jsoup's `parse()` is a CPU-bound blocking call, so it runs on an
 * injected IO dispatcher to avoid blocking the coroutine event loop.
 *
 * Per-host politeness throttling is handled transparently by
 * [PerHostThrottlingFilter], which is registered as a WebClient filter
 * and enforces a minimum delay between consecutive requests to the same host.
 */
@Component
class HtmlFetcher(
    webClientBuilder: WebClient.Builder,
    scraperProperties: ScraperProperties,
    @Qualifier("ioDispatcher") private val ioDispatcher: CoroutineDispatcher
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Shared WebClient instance configured with a response timeout, a browser-like User-Agent,
     * and a [PerHostThrottlingFilter] for politeness rate limiting.
     *
     * The response body size limit is controlled by the standard `spring.http.codecs.max-in-memory-size`
     * property (defaults to 256KB; set to 2MB in application.yaml for scraping large venue pages).
     */
    private val webClient: WebClient =
        webClientBuilder
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create().responseTimeout(scraperProperties.responseTimeout)
                )
            ).defaultHeader(
                "User-Agent",
                "Mozilla/5.0 (compatible; EventChecker/1.0; +https://github.com/enorm-labs/event-checker)"
            ).filter(
                PerHostThrottlingFilter(scraperProperties.politeDelayMillis)
            ).build()

    /**
     * Fetches and parses HTML from [url] with optional conditional-request headers.
     *
     * If both [etag] and [lastModified] are `null`, an unconditional GET is performed.
     * If the server responds with 304 Not Modified, returns [FetchResult.NotModified].
     * Otherwise, parses the HTML body and returns [FetchResult.Success].
     *
     * @param url the page URL to fetch.
     * @param etag the ETag value from a previous fetch (sent as `If-None-Match`).
     * @param lastModified the Last-Modified value from a previous fetch (sent as `If-Modified-Since`).
     * @return a [FetchResult] indicating whether the page was modified or not.
     */
    suspend fun fetch(
        url: String,
        etag: String? = null,
        lastModified: String? = null
    ): FetchResult {
        logger.info { "Fetching $url (etag=$etag, lastModified=$lastModified)" }
        return webClient
            .get()
            // Pass a pre-built URI so WebClient uses the (already percent-encoded) URL verbatim.
            // Passing a String treats it as a URI template and re-encodes '%', double-encoding
            // already-escaped paths (e.g. non-ASCII slugs) into a 404.
            .uri(URI.create(url))
            .apply {
                etag?.let { header("If-None-Match", it) }
                lastModified?.let { header("If-Modified-Since", it) }
            }.awaitExchange { response ->
                handleResponse(response, url)
            }
    }

    /**
     * Fetches and parses HTML from [url] without conditional-request headers.
     *
     * Convenience method for fetching secondary pages (e.g. event detail pages)
     * where change detection is not needed. Returns a parsed Jsoup [Document]
     * with parsing executed on the IO dispatcher to avoid blocking the coroutine
     * event loop.
     *
     * @param url the page URL to fetch.
     * @return a parsed Jsoup [Document].
     */
    suspend fun fetchDocument(url: String): Document {
        val html = fetchHtml(url)
        return parseHtml(html, url)
    }

    /**
     * Fetches raw HTML from [url] without conditional-request headers.
     *
     * Lower-level convenience method for fetching secondary pages (e.g. event detail pages)
     * where change detection is not needed. Prefer [fetchDocument] when a parsed [Document]
     * is needed, as it also moves Jsoup parsing to the IO dispatcher.
     *
     * @param url the page URL to fetch.
     * @return the raw HTML body as a string.
     */
    suspend fun fetchHtml(url: String): String {
        logger.debug { "Fetching detail page: $url" }
        return webClient
            .get()
            // Pass a pre-built URI so WebClient uses the (already percent-encoded) URL verbatim
            // instead of re-encoding '%' and double-encoding non-ASCII slugs into a 404.
            .uri(URI.create(url))
            .awaitExchange { response ->
                // Fail fast on HTTP errors to avoid returning error pages as valid HTML
                if (response.statusCode().isError) {
                    throw HttpFetchException(response.statusCode().value(), url)
                }
                response.awaitBody<String>()
            }
    }

    /**
     * Processes the HTTP response: returns [FetchResult.NotModified] on 304,
     * or parses the body into a [FetchResult.Success] otherwise.
     */
    private suspend fun handleResponse(
        response: ClientResponse,
        url: String
    ): FetchResult {
        if (response.statusCode() == HttpStatus.NOT_MODIFIED) {
            logger.info { "Page not modified: $url" }
            return FetchResult.NotModified
        }

        // Fail fast on HTTP errors to avoid parsing error pages as valid event data
        if (response.statusCode().isError) {
            throw HttpFetchException(response.statusCode().value(), url)
        }

        val body = response.awaitBody<String>()
        val newEtag = response.headers().asHttpHeaders().eTag
        val newLastModified = response.headers().asHttpHeaders().getFirst("Last-Modified")

        logger.info { "Fetched ${body.length} chars from $url (newEtag=$newEtag, newLastModified=$newLastModified)" }

        val document = parseHtml(body, url)
        return FetchResult.Success(
            document = document,
            etag = newEtag,
            lastModified = newLastModified
        )
    }

    /**
     * Parses an HTML string into a Jsoup [Document] on the IO dispatcher
     * to avoid blocking the coroutine event loop.
     */
    private suspend fun parseHtml(
        html: String,
        baseUri: String
    ): Document =
        withContext(ioDispatcher) {
            Jsoup.parse(html, baseUri)
        }
}

/**
 * Result of an HTML fetch operation.
 */
sealed interface FetchResult {
    /** The page has not been modified since the last fetch (304 response). */
    data object NotModified : FetchResult

    /** The page was successfully fetched and parsed. */
    data class Success(
        /** Parsed Jsoup document for CSS selector queries. */
        val document: Document,
        /** New ETag header from the response, if present. */
        val etag: String?,
        /** New Last-Modified header from the response, if present. */
        val lastModified: String?
    ) : FetchResult
}

/**
 * Exception thrown when an HTTP fetch returns an error status code (4xx/5xx).
 *
 * Propagates up to [EventImportService.importFromSource]'s catch block where it is
 * recorded as a failure on the event source.
 */
class HttpFetchException(
    statusCode: Int,
    url: String
) : RuntimeException("HTTP $statusCode when fetching $url")
