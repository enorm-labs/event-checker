package de.norm.events

import io.r2dbc.spi.Readable
import kotlinx.coroutines.reactive.awaitSingle
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
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

/**
 * Base class for BFF controller integration tests.
 *
 * Provides a running server backed by a Testcontainers PostgreSQL database (schema provisioned
 * by the importer's Flyway migrations), a pre-configured [WebTestClient], a [BeforeEach] hook
 * that truncates all tables, and raw-SQL seed helpers. Seeding uses SQL rather than the BFF's
 * lean read entities because those intentionally omit required write-only columns (e.g.
 * `event.source_id`).
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

    protected suspend fun insertVenue(
        name: String,
        slug: String,
        city: String = "Berlin",
        address: String? = null,
        imageUrl: String? = null
    ): Long =
        databaseClient
            .sql(
                "INSERT INTO events.venue (name, slug, city, address, image_url) " +
                    "VALUES (:name, :slug, :city, :address, :imageUrl) RETURNING id"
            ).bind("name", name)
            .bind("slug", slug)
            .bind("city", city)
            .bindOrNull("address", address)
            .bindOrNull("imageUrl", imageUrl)
            .mapId()

    protected suspend fun insertArtist(
        name: String,
        slug: String,
        imageUrl: String? = null,
        description: String? = null
    ): Long =
        databaseClient
            .sql(
                "INSERT INTO events.artist (name, slug, image_url, description) " +
                    "VALUES (:name, :slug, :imageUrl, :description) RETURNING id"
            ).bind("name", name)
            .bind("slug", slug)
            .bindOrNull("imageUrl", imageUrl)
            .bindOrNull("description", description)
            .mapId()

    protected suspend fun insertPromoter(
        name: String,
        slug: String
    ): Long =
        databaseClient
            .sql("INSERT INTO events.promoter (name, slug) VALUES (:name, :slug) RETURNING id")
            .bind("name", name)
            .bind("slug", slug)
            .mapId()

    protected suspend fun insertGenreTag(
        name: String,
        slug: String
    ): Long =
        databaseClient
            .sql("INSERT INTO events.genre_tag (name, slug) VALUES (:name, :slug) RETURNING id")
            .bind("name", name)
            .bind("slug", slug)
            .mapId()

    @Suppress("LongParameterList")
    protected suspend fun insertEvent(
        venueId: Long,
        title: String,
        slug: String,
        eventDate: LocalDate,
        sourceId: String = "test:$slug",
        subtitle: String? = null,
        eventType: String = "CONCERT",
        startTime: LocalTime? = null,
        pricePresale: BigDecimal? = null,
        genre: String? = null
    ): Long =
        databaseClient
            .sql(
                "INSERT INTO events.event (venue_id, title, subtitle, slug, event_date, start_time, source_id, event_type, price_presale, genre) " +
                    "VALUES (:venueId, :title, :subtitle, :slug, :eventDate, :startTime, :sourceId, :eventType, :pricePresale, :genre) RETURNING id"
            ).bind("venueId", venueId)
            .bind("title", title)
            .bindOrNull("subtitle", subtitle)
            .bind("slug", slug)
            .bind("eventDate", eventDate)
            .bindOrNull("startTime", startTime, LocalTime::class.java)
            .bind("sourceId", sourceId)
            .bind("eventType", eventType)
            .bindOrNull("pricePresale", pricePresale, BigDecimal::class.java)
            .bindOrNull("genre", genre)
            .mapId()

    protected suspend fun linkArtist(
        eventId: Long,
        artistId: Long,
        role: String = "HEADLINER",
        billingOrder: Int = 0
    ) {
        databaseClient
            .sql(
                "INSERT INTO events.event_artist (event_id, artist_id, role, billing_order) " +
                    "VALUES (:eventId, :artistId, :role, :billingOrder)"
            ).bind("eventId", eventId)
            .bind("artistId", artistId)
            .bind("role", role)
            .bind("billingOrder", billingOrder)
            .await()
    }

    protected suspend fun linkPromoter(
        eventId: Long,
        promoterId: Long
    ) {
        databaseClient
            .sql("INSERT INTO events.event_promoter (event_id, promoter_id) VALUES (:eventId, :promoterId)")
            .bind("eventId", eventId)
            .bind("promoterId", promoterId)
            .await()
    }

    protected suspend fun linkGenre(
        eventId: Long,
        genreTagId: Long
    ) {
        databaseClient
            .sql("INSERT INTO events.event_genre_tag (event_id, genre_tag_id) VALUES (:eventId, :genreTagId)")
            .bind("eventId", eventId)
            .bind("genreTagId", genreTagId)
            .await()
    }

    private suspend fun DatabaseClient.GenericExecuteSpec.mapId(): Long = map { row: Readable -> row.get(0, Long::class.javaObjectType)!! }.one().awaitSingle()

    private fun DatabaseClient.GenericExecuteSpec.bindOrNull(
        name: String,
        value: String?
    ): DatabaseClient.GenericExecuteSpec = if (value != null) bind(name, value) else bindNull(name, String::class.java)

    private fun DatabaseClient.GenericExecuteSpec.bindOrNull(
        name: String,
        value: Any?,
        type: Class<*>
    ): DatabaseClient.GenericExecuteSpec = if (value != null) bind(name, value) else bindNull(name, type)
}
