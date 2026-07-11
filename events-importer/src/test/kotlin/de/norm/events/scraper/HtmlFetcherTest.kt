package de.norm.events.scraper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

/**
 * Tests for [HtmlFetcher] that exercise the real Spring [WebClient] request
 * pipeline against a local [MockWebServer]. Verifies URL handling, conditional
 * requests and error propagation end to end — no HTTP mocking of the client.
 */
class HtmlFetcherTest {
    private lateinit var server: MockWebServer

    private val fetcher: HtmlFetcher by lazy {
        HtmlFetcher(
            // The real shared bean, built from the production config — no politeness delay so
            // tests aren't slowed by the per-host throttle.
            webClient =
                ScraperHttpClientConfig().scraperWebClient(
                    webClientBuilder = WebClient.builder(),
                    scraperProperties = ScraperProperties(politeDelayMillis = 0)
                ),
            ioDispatcher = Dispatchers.IO
        )
    }

    @BeforeEach
    fun startServer() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun stopServer() {
        server.shutdown()
    }

    /** Base URL of the mock server without a trailing slash, e.g. `http://localhost:12345`. */
    private fun baseUrl(): String = server.url("/").toString().trimEnd('/')

    @Nested
    inner class PreEncodedUrls {
        // The exact slug from the Badehaus regression: an already percent-encoded Arabic
        // title. Passing it as a WebClient URI *template* would re-encode the '%' signs
        // (%d8 -> %25d8), double-encoding the path into a 404 on the origin server.
        private val encodedPath =
            "/events/sahra-party-%d8%ad%d9%81%d9%84%d8%a9-%d8%b3%d9%87%d8%b1%d8%a9-pride-of-arab-women/"

        @Test
        fun `fetchDocument sends an already-encoded path verbatim without double-encoding`() =
            runTest {
                server.enqueue(MockResponse().setResponseCode(200).setBody("<html><body>ok</body></html>"))

                fetcher.fetchDocument(baseUrl() + encodedPath)

                val recorded = server.takeRequest()
                recorded.path shouldBe encodedPath
                // Guards against the regression specifically: no '%' was re-escaped to '%25'.
                recorded.path shouldNotContain "%25"
            }

        @Test
        fun `fetch sends an already-encoded path verbatim without double-encoding`() =
            runTest {
                server.enqueue(MockResponse().setResponseCode(200).setBody("<html><body>ok</body></html>"))

                fetcher.fetch(baseUrl() + encodedPath)

                val recorded = server.takeRequest()
                recorded.path shouldBe encodedPath
                recorded.path shouldNotContain "%25"
            }
    }

    @Nested
    inner class RawBodyFetching {
        @Test
        fun `fetchHtml returns the response body verbatim`() =
            runTest {
                val body = "<html><body>ok</body></html>"
                server.enqueue(MockResponse().setResponseCode(200).setBody(body))

                fetcher.fetchHtml(baseUrl() + "/page") shouldBe body
            }
    }

    @Nested
    inner class ErrorHandling {
        @Test
        fun `fetchHtml throws HttpFetchException on a 404`() =
            runTest {
                server.enqueue(MockResponse().setResponseCode(404))

                val url = baseUrl() + "/missing"
                val exception =
                    shouldThrow<HttpFetchException> {
                        fetcher.fetchHtml(url)
                    }

                exception.message!! shouldContain "HTTP 404"
                exception.message!! shouldContain url
            }
    }
}
