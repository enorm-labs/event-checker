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
    fun `GET venues filters by district`(): Unit =
        runBlocking {
            insertVenue("Astra", "astra", district = "friedrichshain-kreuzberg")
            insertVenue("Lido", "lido", district = "friedrichshain-kreuzberg")
            insertVenue("Berghain", "berghain", district = "mitte")

            webTestClient
                .get()
                .uri("/venues?district=mitte")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.totalElements")
                .isEqualTo(1)
                .jsonPath("$.content[0].slug")
                .isEqualTo("berghain")
        }

    @Test
    fun `GET venues combines name query and district filter`(): Unit =
        runBlocking {
            insertVenue("Astra Kulturhaus", "astra", district = "friedrichshain-kreuzberg")
            insertVenue("Astra Bar", "astra-bar", district = "mitte")
            insertVenue("Lido", "lido", district = "friedrichshain-kreuzberg")

            webTestClient
                .get()
                .uri("/venues?q=astra&district=friedrichshain-kreuzberg")
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
    fun `GET venues ignores an unknown or malicious sort parameter`(): Unit =
        runBlocking {
            insertVenue("Astra", "astra")
            insertVenue("Lido", "lido")

            // Swagger UI's array placeholder `["string"]` and an injection attempt both contain
            // characters Spring Data rejects; without the sort whitelist these would surface as a
            // 500. They are dropped, so the request succeeds with the default name-ascending order.
            for (sort in listOf("""["string"]""", "name; DROP TABLE events.venue;--")) {
                webTestClient
                    .get()
                    .uri("/venues?sort={sort}", sort)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody()
                    .jsonPath("$.totalElements")
                    .isEqualTo(2)
                    .jsonPath("$.content[0].slug")
                    .isEqualTo("astra")
            }
        }

    @Test
    fun `GET venues honours a whitelisted sort property`(): Unit =
        runBlocking {
            insertVenue("Astra", "astra")
            insertVenue("Lido", "lido")

            webTestClient
                .get()
                .uri("/venues?sort=name,desc")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.content[0].slug")
                .isEqualTo("lido")
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
