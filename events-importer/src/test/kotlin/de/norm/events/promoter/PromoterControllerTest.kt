package de.norm.events.promoter

import de.norm.events.BaseControllerTest
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody

class PromoterControllerTest : BaseControllerTest() {
    /** Creates a promoter via the API and returns the persisted [PromoterResponse]. */
    private fun createPromoter(request: PromoterRequest = PromoterRequestFixtures.concerts36()): PromoterResponse =
        webTestClient
            .post()
            .uri("/api/admin/promoters")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody<PromoterResponse>()
            .returnResult()
            .responseBody!!

    /** Deletes a promoter via the API. */
    private fun deletePromoter(id: Long) {
        webTestClient
            .delete()
            .uri("/api/admin/promoters/$id")
            .exchange()
            .expectStatus()
            .isNoContent
    }

    @Test
    fun `POST, GET, PUT, DELETE promoter lifecycle`() {
        // Create
        val created = createPromoter()

        created.name shouldBe "36 Concerts"
        created.slug shouldBe "36-concerts"

        val id = created.id

        // Read
        webTestClient
            .get()
            .uri("/api/admin/promoters/$id")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PromoterResponse>()
            .consumeWith { result ->
                result.responseBody!!.name shouldBe "36 Concerts"
            }

        // Update
        webTestClient
            .put()
            .uri("/api/admin/promoters/$id")
            .bodyValue(PromoterRequestFixtures.concerts36(name = "36 Concerts GmbH"))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PromoterResponse>()
            .consumeWith { result ->
                val promoter = result.responseBody!!
                promoter.name shouldBe "36 Concerts GmbH"
                promoter.slug shouldBe "36-concerts-gmbh"
            }

        // Delete
        deletePromoter(id)

        // Verify deleted
        webTestClient
            .get()
            .uri("/api/admin/promoters/$id")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `GET promoter by non-existent ID returns 404`() {
        webTestClient
            .get()
            .uri("/api/admin/promoters/99999")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `GET all promoters returns list`() {
        val created = createPromoter(PromoterRequestFixtures.create(name = "Goodlive"))

        val promoters =
            webTestClient
                .get()
                .uri("/api/admin/promoters")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<List<PromoterResponse>>()
                .returnResult()
                .responseBody!!

        promoters.size shouldBeGreaterThanOrEqual 1
        promoters.map { it.id } shouldContain created.id
    }

    @Test
    fun `POST promoter with blank name returns 400`() {
        webTestClient
            .post()
            .uri("/api/admin/promoters")
            .bodyValue(PromoterRequestFixtures.create(name = ""))
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `POST promoter with whitespace-only name returns 400`() {
        webTestClient
            .post()
            .uri("/api/admin/promoters")
            .bodyValue(PromoterRequestFixtures.create(name = "   "))
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `PUT promoter with blank name returns 400`() {
        val created = createPromoter()

        webTestClient
            .put()
            .uri("/api/admin/promoters/${created.id}")
            .bodyValue(PromoterRequestFixtures.create(name = ""))
            .exchange()
            .expectStatus()
            .isBadRequest
    }
}
