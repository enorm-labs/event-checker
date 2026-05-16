package de.norm.events

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Base class for importer controller integration tests.
 *
 * Provides a running server with a Testcontainers-backed PostgreSQL database,
 * a pre-configured [WebTestClient], and a [BeforeEach] hook that truncates all
 * tables so every test starts with a clean database.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(PostgresTestcontainersConfiguration::class)
abstract class BaseControllerTest {
    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    protected lateinit var databaseClient: DatabaseClient

    protected val webTestClient: WebTestClient by lazy {
        WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    /** Truncates all domain tables before each test to ensure a clean state. */
    @BeforeEach
    fun cleanUp() =
        runBlocking {
            databaseClient
                .sql(
                    "TRUNCATE TABLE events.event_source, events.event_genre_tag, events.event_promoter, events.event_artist, " +
                        "events.event, events.genre_tag, events.promoter, events.artist, events.venue CASCADE"
                ).await()
        }
}
