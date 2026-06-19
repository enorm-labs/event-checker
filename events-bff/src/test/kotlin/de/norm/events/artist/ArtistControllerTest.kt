package de.norm.events.artist

import de.norm.events.BaseControllerTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ArtistControllerTest : BaseControllerTest() {
    @Test
    fun `GET artists lists artists with pagination metadata`(): Unit =
        runBlocking {
            insertArtist("The Adicts", "the-adicts")
            insertArtist("Maid Of Ace", "maid-of-ace")

            webTestClient
                .get()
                .uri("/artists")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.totalElements")
                .isEqualTo(2)
                // default sort by name asc
                .jsonPath("$.content[0].slug")
                .isEqualTo("maid-of-ace")
        }

    @Test
    fun `GET artists filters by case-insensitive name query`(): Unit =
        runBlocking {
            insertArtist("The Adicts", "the-adicts")
            insertArtist("Maid Of Ace", "maid-of-ace")

            webTestClient
                .get()
                .uri("/artists?q=adicts")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.totalElements")
                .isEqualTo(1)
                .jsonPath("$.content[0].slug")
                .isEqualTo("the-adicts")
        }

    @Test
    fun `GET artist by slug returns detail`(): Unit =
        runBlocking {
            insertArtist("The Adicts", "the-adicts", description = "Punk band from Ipswich")

            webTestClient
                .get()
                .uri("/artists/the-adicts")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.slug")
                .isEqualTo("the-adicts")
                .jsonPath("$.name")
                .isEqualTo("The Adicts")
                .jsonPath("$.description")
                .isEqualTo("Punk band from Ipswich")
        }

    @Test
    fun `GET artist by unknown slug returns 404`() {
        webTestClient
            .get()
            .uri("/artists/nope")
            .exchange()
            .expectStatus()
            .isNotFound
    }
}
