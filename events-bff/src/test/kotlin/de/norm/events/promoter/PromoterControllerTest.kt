package de.norm.events.promoter

import de.norm.events.BaseControllerTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class PromoterControllerTest : BaseControllerTest() {
    @Test
    fun `GET promoters lists promoters with pagination metadata`(): Unit =
        runBlocking {
            insertPromoter("36 Concerts", "36-concerts")
            insertPromoter("Goodlive", "goodlive")

            webTestClient
                .get()
                .uri("/promoters")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.totalElements")
                .isEqualTo(2)
                // default sort by name asc
                .jsonPath("$.content[0].slug")
                .isEqualTo("36-concerts")
        }

    @Test
    fun `GET promoters filters by case-insensitive name query`(): Unit =
        runBlocking {
            insertPromoter("36 Concerts", "36-concerts")
            insertPromoter("Goodlive", "goodlive")

            webTestClient
                .get()
                .uri("/promoters?q=good")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.totalElements")
                .isEqualTo(1)
                .jsonPath("$.content[0].slug")
                .isEqualTo("goodlive")
        }

    @Test
    fun `GET promoters ignores an unknown or malicious sort parameter`(): Unit =
        runBlocking {
            insertPromoter("36 Concerts", "36-concerts")
            insertPromoter("Goodlive", "goodlive")

            // Swagger UI's array placeholder `["string"]` and an injection attempt both contain
            // characters Spring Data rejects; without the sort whitelist these would surface as a
            // 500. They are dropped, so the request succeeds with the default name-ascending order.
            for (sort in listOf("""["string"]""", "name; DROP TABLE events.promoter;--")) {
                webTestClient
                    .get()
                    .uri("/promoters?sort={sort}", sort)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody()
                    .jsonPath("$.totalElements")
                    .isEqualTo(2)
                    .jsonPath("$.content[0].slug")
                    .isEqualTo("36-concerts")
            }
        }

    @Test
    fun `GET promoters honours a whitelisted sort property`(): Unit =
        runBlocking {
            insertPromoter("36 Concerts", "36-concerts")
            insertPromoter("Goodlive", "goodlive")

            webTestClient
                .get()
                .uri("/promoters?sort=name,desc")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.content[0].slug")
                .isEqualTo("goodlive")
        }

    @Test
    fun `GET promoter by slug returns detail`(): Unit =
        runBlocking {
            insertPromoter(
                "36 Concerts",
                "36-concerts",
                description = "The in-house agency of Lido, Astra and Bi Nuu.",
                descriptionLanguage = "en",
                descriptionAlt = "Die Hausagentur von Lido, Astra und Bi Nuu.",
                descriptionAltLanguage = "de"
            )

            webTestClient
                .get()
                .uri("/promoters/36-concerts")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.slug")
                .isEqualTo("36-concerts")
                .jsonPath("$.name")
                .isEqualTo("36 Concerts")
                .jsonPath("$.description")
                .isEqualTo("The in-house agency of Lido, Astra and Bi Nuu.")
                .jsonPath("$.descriptionLanguage")
                .isEqualTo("en")
                .jsonPath("$.descriptionAlt")
                .isEqualTo("Die Hausagentur von Lido, Astra und Bi Nuu.")
                .jsonPath("$.descriptionAltLanguage")
                .isEqualTo("de")
        }

    // The summary embedded in events and the list stays compact: no description there.
    @Test
    fun `GET promoters list carries no description`(): Unit =
        runBlocking {
            insertPromoter("36 Concerts", "36-concerts", description = "Text", descriptionLanguage = "en")

            webTestClient
                .get()
                .uri("/promoters")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.content[0].description")
                .doesNotExist()
        }

    @Test
    fun `GET promoter by unknown slug returns 404`() {
        webTestClient
            .get()
            .uri("/promoters/nope")
            .exchange()
            .expectStatus()
            .isNotFound
    }
}
