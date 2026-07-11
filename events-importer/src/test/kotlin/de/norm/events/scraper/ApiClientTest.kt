package de.norm.events.scraper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

/**
 * Tests for [ApiClient] that exercise the real Spring [WebClient] request pipeline against a
 * local [MockWebServer]. Verifies raw-body return, verbatim URL handling, and error
 * propagation end to end — no HTTP mocking of the client.
 */
class ApiClientTest {
    private lateinit var server: MockWebServer

    private val apiClient: ApiClient by lazy {
        ApiClient(
            // The real shared bean, built from the production config — no politeness delay so
            // tests aren't slowed by the per-host throttle.
            webClient =
                ScraperHttpClientConfig().scraperWebClient(
                    webClientBuilder = WebClient.builder(),
                    scraperProperties = ScraperProperties(politeDelayMillis = 0)
                )
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

    @Test
    fun `fetchJson returns the response body verbatim for a JSON payload`() =
        runTest {
            val body = """{"items":[{"title":"ok"}]}"""
            server.enqueue(MockResponse().setResponseCode(200).setBody(body))

            apiClient.fetchJson(baseUrl() + "/api") shouldBe body
        }

    @Test
    fun `fetchJson sends the query string verbatim without re-encoding`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

            // The Elfsight boot URL carries the widget id as a query parameter; it must arrive intact.
            val path = "/p/boot/?w=e767cbbe-0026-4173-a511-5aaa105ed563"
            apiClient.fetchJson(baseUrl() + path)

            val recorded = server.takeRequest()
            recorded.path shouldBe path
            recorded.path shouldNotContain "%25"
        }

    @Test
    fun `fetchJson throws HttpFetchException on a 404`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(404))

            val url = baseUrl() + "/missing"
            val exception =
                shouldThrow<HttpFetchException> {
                    apiClient.fetchJson(url)
                }

            exception.message!! shouldContain "HTTP 404"
            exception.message!! shouldContain url
        }
}
