package de.norm.events.genretag

import de.norm.events.BaseControllerTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class GenreTagControllerTest : BaseControllerTest() {
    @Test
    fun `GET genres lists all genre tags alphabetically`(): Unit =
        runBlocking {
            insertGenreTag("Techno", "techno")
            insertGenreTag("Hip Hop", "hip-hop")
            insertGenreTag("Punk", "punk")

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
                .jsonPath("$[1].name")
                .isEqualTo("Punk")
                .jsonPath("$[2].name")
                .isEqualTo("Techno")
        }
}
