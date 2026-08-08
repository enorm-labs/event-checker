package de.norm.events.meta

import de.norm.events.BaseControllerTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/**
 * Verifies the endpoint is actually wired and public. The mapping itself — including the fallback
 * when the build stamped nothing — is covered by [MetaResponseTest], because whether
 * `build-info.properties` is on the test classpath is a property of the build, not of the test.
 */
class MetaControllerTest : BaseControllerTest() {
    @Test
    fun `GET meta reports a version without requiring authentication`(): Unit =
        runBlocking {
            webTestClient
                .get()
                .uri("/meta")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.version")
                .isNotEmpty
        }

    @Test
    fun `GET meta does not leak the internal artifact identifiers actuator would expose`(): Unit =
        runBlocking {
            // The reason this endpoint exists rather than publishing /actuator/info: that payload
            // carries build.group / build.artifact / build.name, which are internal names with no
            // business in a public API (docs/LEGAL.md §4.4).
            webTestClient
                .get()
                .uri("/meta")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.group")
                .doesNotExist()
                .jsonPath("$.artifact")
                .doesNotExist()
                .jsonPath("$.name")
                .doesNotExist()
        }
}
