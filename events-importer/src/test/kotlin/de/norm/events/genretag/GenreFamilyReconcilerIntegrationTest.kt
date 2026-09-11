package de.norm.events.genretag

import de.norm.events.BaseControllerTest
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * [GenreFamilyReconciler] against a real database: a tag stored with a stale or missing family
 * takes the map's value on the next start, and a tag the map does not name is left at `null`.
 */
class GenreFamilyReconcilerIntegrationTest : BaseControllerTest() {
    @Autowired
    private lateinit var reconciler: GenreFamilyReconciler

    @Autowired
    private lateinit var genreTagRepository: GenreTagRepository

    @Test
    fun `insert stores the family the map names`(): Unit =
        runBlocking {
            genreTagRepository.insertIfAbsent("Techno", "techno", genreFamily("techno")?.slug)

            genreTagRepository.findBySlug("techno")?.family shouldBe "electronic"
        }

    @Test
    fun `reconcile rewrites stale families and counts the rows it changed`(): Unit =
        runBlocking {
            // Stored before the map knew better: one wrong, one missing, one already right, one orphan.
            genreTagRepository.insertIfAbsent("Techno", "techno", "rock")
            genreTagRepository.insertIfAbsent("House", "house", null)
            genreTagRepository.insertIfAbsent("Punk", "punk", "punk")
            genreTagRepository.insertIfAbsent("Ping Pong", "ping-pong", null)

            reconciler.reconcile() shouldBe 2

            genreTagRepository.findBySlug("techno")?.family shouldBe "electronic"
            genreTagRepository.findBySlug("house")?.family shouldBe "electronic"
            genreTagRepository.findBySlug("punk")?.family shouldBe "punk"
            genreTagRepository.findBySlug("ping-pong")?.family.shouldBeNull()

            // A second pass has nothing left to do.
            reconciler.reconcile() shouldBe 0
        }
}
