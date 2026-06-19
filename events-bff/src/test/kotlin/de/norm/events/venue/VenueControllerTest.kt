package de.norm.events.venue

import de.norm.events.BaseControllerTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class VenueControllerTest : BaseControllerTest() {
    @Test
    fun `GET venues lists venues with pagination metadata`(): Unit =
        runBlocking {
            insertVenue("Astra", "astra")
            insertVenue("Lido", "lido")

            webTestClient
                .get()
                .uri("/venues")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.totalElements")
                .isEqualTo(2)
                .jsonPath("$.content.length()")
                .isEqualTo(2)
                // default sort by name asc
                .jsonPath("$.content[0].slug")
                .isEqualTo("astra")
        }

    @Test
    fun `GET venues filters by case-insensitive name query`(): Unit =
        runBlocking {
            insertVenue("Astra Kulturhaus", "astra")
            insertVenue("Lido", "lido")

            webTestClient
                .get()
                .uri("/venues?q=astra")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.totalElements")
                .isEqualTo(1)
                .jsonPath("$.content[0].slug")
                .isEqualTo("astra")
        }

    @Test
    fun `GET venue by slug returns detail`(): Unit =
        runBlocking {
            insertVenue("Astra", "astra", address = "Revaler Str. 99")

            webTestClient
                .get()
                .uri("/venues/astra")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.slug")
                .isEqualTo("astra")
                .jsonPath("$.name")
                .isEqualTo("Astra")
                .jsonPath("$.address")
                .isEqualTo("Revaler Str. 99")
        }

    @Test
    fun `GET venue by unknown slug returns 404`() {
        webTestClient
            .get()
            .uri("/venues/nope")
            .exchange()
            .expectStatus()
            .isNotFound
    }
}
