package de.norm.events.event

import de.norm.events.BaseControllerTest
import de.norm.events.venue.VenueRequestFixtures
import de.norm.events.venue.VenueResponse
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.r2dbc.core.await
import org.springframework.test.web.reactive.server.expectBody

/**
 * The stored-description backfill, against a real database.
 *
 * **A mock cannot fail the way this endpoint failed.** The first version collected the cursor and
 * saved from inside `collect`, so the write waited on the connection the open cursor was holding.
 * There was no error, no query and nothing in the log — the request simply never returned, and the
 * unit test passed because `flowOf` has no connection to hold. Only a real R2DBC connection
 * reproduces it, which is why this suite exists at all.
 */
class DescriptionLanguageBackfillIntegrationTest : BaseControllerTest() {
    @Test
    @DisplayName("it classifies stored descriptions and returns, rather than deadlocking on the cursor")
    fun `classifies stored descriptions`(): Unit =
        runBlocking {
            val venueId = createVenue().id
            insertEvent(venueId, "de-event", GERMAN_DESCRIPTION)
            insertEvent(venueId, "en-event", ENGLISH_DESCRIPTION)
            insertEvent(venueId, "short-event", "Doors 19:30")
            insertEvent(venueId, "no-description-event", null)

            webTestClient
                .post()
                .uri("/api/admin/events/detect-languages")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.german")
                .isEqualTo(1)
                .jsonPath("$.english")
                .isEqualTo(1)
                // The two-line note. An event with no description is not a candidate at all.
                .jsonPath("$.unknown")
                .isEqualTo(1)

            language("de-event") shouldBeLanguage "de"
            language("en-event") shouldBeLanguage "en"
            language("short-event") shouldBeLanguage null
        }

    // Running it twice is how an operator uses it, and the second run must find only what the first
    // could not call.
    @Test
    @DisplayName("a second run reclassifies nothing it already decided")
    fun `is idempotent`(): Unit =
        runBlocking {
            val venueId = createVenue().id
            insertEvent(venueId, "de-event", GERMAN_DESCRIPTION)

            repeat(2) {
                webTestClient
                    .post()
                    .uri("/api/admin/events/detect-languages")
                    .exchange()
                    .expectStatus()
                    .isOk
            }

            webTestClient
                .post()
                .uri("/api/admin/events/detect-languages")
                .exchange()
                .expectBody()
                .jsonPath("$.german")
                .isEqualTo(0)
        }

    private suspend fun language(slug: String): String? =
        databaseClient
            .sql("SELECT description_language FROM events.event WHERE slug = :slug")
            .bind("slug", slug)
            .map { row -> row.get("description_language", String::class.java) ?: NO_LANGUAGE }
            .one()
            .awaitSingle()
            .takeIf { it != NO_LANGUAGE }

    private infix fun String?.shouldBeLanguage(expected: String?) {
        check(this == expected) { "expected language $expected but was $this" }
    }

    private fun createVenue(): VenueResponse =
        webTestClient
            .post()
            .uri("/api/admin/venues")
            .bodyValue(VenueRequestFixtures.astra())
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody<VenueResponse>()
            .returnResult()
            .responseBody!!

    private suspend fun insertEvent(
        venueId: Long,
        slug: String,
        description: String?
    ) {
        databaseClient
            .sql(
                "INSERT INTO events.event (venue_id, title, slug, event_date, source_id, description) " +
                    "VALUES (:venueId, :title, :slug, DATE '2026-09-07', :sourceId, :description)"
            ).bind("venueId", venueId)
            .bind("title", slug)
            .bind("slug", slug)
            .bind("sourceId", "test:$slug")
            .let { if (description == null) it.bindNull("description", String::class.java) else it.bind("description", description) }
            .await()
    }

    private companion object {
        /** R2DBC's `map` cannot emit null, so a sentinel carries "no language" out of the query. */
        const val NO_LANGUAGE = "<none>"
        const val GERMAN_DESCRIPTION =
            "Im UFO treffen Berliner Straßenrap, kompromisslose Beats und jede Menge Energie aufeinander, wenn der " +
                "Rapper in seiner Heimatstadt Halt macht."
        const val ENGLISH_DESCRIPTION =
            "Berlin street rap, uncompromising beats and plenty of energy come together when the rapper stops by in " +
                "his hometown for one night."
    }
}
