package de.norm.events.scraper

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import java.net.URI

/**
 * Reactive HTML fetcher with conditional-request support and Jsoup parsing.
 *
 * Uses the shared, politeness-throttled scraper [WebClient] ([SCRAPER_WEB_CLIENT]) for
 * non-blocking HTTP fetching and Jsoup for HTML parsing. Supports ETag / Last-Modified
 * conditional headers to skip re-downloading pages that haven't changed since the last import.
 *
 * Jsoup's `parse()` is a CPU-bound blocking call, so it runs on an
 * injected IO dispatcher to avoid blocking the coroutine event loop.
 *
 * Venues whose events come from a JSON/API source rather than a scrapeable HTML page use
 * [ApiClient] instead — it shares the same [WebClient] bean (and therefore the same per-host
 * throttle and User-Agent), so this class stays focused purely on HTML.
 *
 * Per-host politeness throttling is handled transparently by [PerHostThrottlingFilter],
 * registered as a filter on the shared [WebClient] (see [ScraperHttpClientConfig]).
 */
@Component
class HtmlFetcher(
    @Qualifier(SCRAPER_WEB_CLIENT) private val webClient: WebClient,
    @Qualifier("ioDispatcher") private val ioDispatcher: CoroutineDispatcher
) {
    private val logger = KotlinLogging.logger {}

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
     * is needed, as it also moves Jsoup parsing to the IO dispatcher. Fails fast with
     * [HttpFetchException] on any 4xx/5xx so error pages are never parsed as valid event data.
     *
     * @param url the page URL to fetch.
     * @return the raw HTML body as a string.
     */
    suspend fun fetchHtml(url: String): String {
        logger.debug { "Fetching HTML body: $url" }
        return webClient
            .get()
            // Pass a pre-built URI so WebClient uses the (already percent-encoded) URL verbatim
            // instead of re-encoding '%' and double-encoding non-ASCII slugs into a 404.
            .uri(URI.create(url))
            .awaitExchange { response ->
                // Fail fast on HTTP errors to avoid returning error pages as valid data
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
