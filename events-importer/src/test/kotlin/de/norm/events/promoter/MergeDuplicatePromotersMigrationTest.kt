package de.norm.events.promoter

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager

/**
 * Runs V023 against the rows it names, on a database migrated to just before it.
 *
 * The migration is keyed on slugs read from staging, and a misspelt one updates no row while
 * Flyway records success (#987). Nothing seeds promoters, so `MigrationSlugTest` cannot check
 * them; this test plants the shapes the four steps handle and reads back what each one did.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MergeDuplicatePromotersMigrationTest {
    private val postgres = PostgreSQLContainer("postgres:18.3-alpine")
    private lateinit var connection: Connection

    @BeforeAll
    fun migrateAndPlant() {
        postgres.start()
        connection = DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)
        flyway("22").migrate()
        connection.createStatement().use { statement ->
            statement.execute("SET search_path TO events")
            statement.execute("INSERT INTO venue (name, slug) VALUES ('Fixture', 'fixture')")
            // A merge onto a survivor that exists, with one event linked to both sides.
            plantPromoter(statement, "Trinity", "trinity")
            plantPromoter(statement, "Trinity Music", "trinity-music")
            plantEvent(statement, "e1", "trinity")
            plantEvent(statement, "e2", "trinity", "trinity-music")
            // A rename only: same slug, corrected display name.
            plantPromoter(statement, "Tv Noir", "tv-noir")
            plantEvent(statement, "e3", "tv-noir")
            // Two losers whose survivor does not exist: one is renamed, the other merged into it.
            plantPromoter(statement, "beav boloney & little league shows", "beav-boloney-little-league-shows")
            plantPromoter(statement, "beav boloney, wild wax & little league shows", "beav-boloney-wild-wax-little-league-shows")
            plantEvent(statement, "e4", "beav-boloney-little-league-shows")
            plantEvent(statement, "e5", "beav-boloney-wild-wax-little-league-shows")
            // A row the migration does not name, left alone.
            plantPromoter(statement, "Listen", "listen")
            plantEvent(statement, "e6", "listen")
        }
        flyway("23").migrate()
    }

    @AfterAll
    fun stop() {
        connection.close()
        postgres.stop()
    }

    @Test
    fun `moves the loser's events onto the existing survivor and deletes the loser`() {
        promoters().containsKey("trinity") shouldBe false
        promoters()["trinity-music"] shouldBe "Trinity Music"
        eventsOf("trinity-music") shouldContainExactlyInAnyOrder listOf("e1", "e2")
    }

    @Test
    fun `renames a survivor in place`() {
        promoters()["tv-noir"] shouldBe "TV Noir"
        eventsOf("tv-noir") shouldContainExactlyInAnyOrder listOf("e3")
    }

    @Test
    fun `makes a missing survivor out of one loser and merges the other into it`() {
        promoters()["beav-boloney"] shouldBe "beav boloney"
        promoters().keys.none { it.contains("little-league") } shouldBe true
        eventsOf("beav-boloney") shouldContainExactlyInAnyOrder listOf("e4", "e5")
    }

    @Test
    fun `leaves a row it does not name alone`() {
        promoters()["listen"] shouldBe "Listen"
        eventsOf("listen") shouldContainExactlyInAnyOrder listOf("e6")
    }

    @Test
    fun `keeps every event linked to exactly the promoters it had, with no duplicate link`() {
        val links =
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT event_id, promoter_id FROM events.event_promoter").use { rows ->
                    generateSequence { if (rows.next()) rows.getLong(1) to rows.getLong(2) else null }.toList()
                }
            }
        links.distinct().size shouldBe links.size
        links.size shouldBe 6
    }

    private fun flyway(target: String): Flyway =
        Flyway
            .configure()
            .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .schemas("events")
            .target(target)
            .load()

    private fun plantPromoter(
        statement: java.sql.Statement,
        name: String,
        slug: String
    ) {
        statement.execute("INSERT INTO promoter (name, slug) VALUES ('${name.replace("'", "''")}', '$slug')")
    }

    private fun plantEvent(
        statement: java.sql.Statement,
        sourceId: String,
        vararg promoterSlugs: String
    ) {
        statement.execute(
            "INSERT INTO event (venue_id, title, slug, event_date, source_id) " +
                "SELECT id, '$sourceId', '$sourceId', DATE '2026-01-01', '$sourceId' FROM venue WHERE slug = 'fixture'"
        )
        for (slug in promoterSlugs) {
            statement.execute(
                "INSERT INTO event_promoter (event_id, promoter_id) " +
                    "SELECT e.id, p.id FROM event e, promoter p WHERE e.source_id = '$sourceId' AND p.slug = '$slug'"
            )
        }
    }

    private fun promoters(): Map<String, String> =
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT slug, name FROM events.promoter").use { rows ->
                generateSequence { if (rows.next()) rows.getString(1) to rows.getString(2) else null }.toMap()
            }
        }

    private fun eventsOf(slug: String): List<String> =
        connection.createStatement().use { statement ->
            statement
                .executeQuery(
                    "SELECT e.source_id FROM events.event e JOIN events.event_promoter ep ON ep.event_id = e.id " +
                        "JOIN events.promoter p ON p.id = ep.promoter_id WHERE p.slug = '$slug'"
                ).use { rows -> generateSequence { if (rows.next()) rows.getString(1) else null }.toList() }
        }
}
