package de.norm.events.genretag

import de.norm.events.BaseControllerTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class GenreTagControllerTest : BaseControllerTest() {
    @Test
    fun `GET genres lists all genre tags alphabetically`(): Unit =
        runBlocking {
            insertGenreTag("Techno", "techno", family = "electronic")
            insertGenreTag("Hip Hop", "hip-hop", family = "hip-hop")
            insertGenreTag("Ping Pong", "ping-pong")

            webTestClient
                .get()
                .uri("/genres")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.length()")
                .isEqualTo(3)
                .jsonPath("$[0].name")
                .isEqualTo("Hip Hop")
                .jsonPath("$[0].family")
                .isEqualTo("hip-hop")
                .jsonPath("$[1].name")
                .isEqualTo("Ping Pong")
                // A tag the importer places in no family says so, and the frontend hides it.
                .jsonPath("$[1].family")
                .isEqualTo(null)
                .jsonPath("$[2].name")
                .isEqualTo("Techno")
                .jsonPath("$[2].family")
                .isEqualTo("electronic")
        }
}
