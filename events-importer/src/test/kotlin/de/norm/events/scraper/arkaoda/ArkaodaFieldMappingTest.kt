package de.norm.events.scraper.arkaoda

import de.norm.events.event.EventType
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Unit tests for the field rules the two arkaoda scrapers share.
 *
 * Every title asserted here is a real one taken from the venue's programme, so the
 * cases document what the narrow artist rule accepts and what it deliberately drops.
 */
class ArkaodaFieldMappingTest {
    private val concert = EventType.CONCERT.name

    @Test
    fun `parses the spaced day-month-year date`() {
        parseArkaodaDate("30 / 07 / 2026") shouldBe LocalDate.of(2026, 7, 30)
        parseArkaodaDate("1 / 9 / 2026") shouldBe LocalDate.of(2026, 9, 1)
    }

    @Test
    fun `returns null for header labels that are not dates`() {
        parseArkaodaDate("Donnerstag") shouldBe null
        parseArkaodaDate("// Konser") shouldBe null
        parseArkaodaDate("") shouldBe null
        parseArkaodaDate(null) shouldBe null
    }

    @Test
    fun `undoes the leaked PHP addslashes escapes`() {
        unescapeAddslashes("""Post Clients & Friends: 7\" Vinyl Release Party""") shouldBe
            """Post Clients & Friends: 7" Vinyl Release Party"""
        unescapeAddslashes("""Seedj presents: \'A Day Behind the Scenes\'""") shouldBe
            "Seedj presents: 'A Day Behind the Scenes'"
        unescapeAddslashes("MNJM") shouldBe "MNJM"
    }

    @Test
    fun `maps the Konser label to a concert`() {
        arkaodaEventType("Konser", "Juana Aguirre") shouldBe concert
        arkaodaEventType("konser", "Juana Aguirre") shouldBe concert
    }

    @Test
    fun `types an unlabelled night from its title instead of defaulting to concert`() {
        arkaodaEventType(null, "Etikett Radio Family Party 001") shouldBe EventType.PARTY.name
        arkaodaEventType(null, "Community Clubnight") shouldBe EventType.PARTY.name
        arkaodaEventType(null, "Meta Rave") shouldBe EventType.PARTY.name
        // No keyword cue at all — never CONCERT, because the venue would have said "Konser".
        arkaodaEventType(null, "MNJM") shouldBe EventType.OTHER.name
        arkaodaEventType(null, "arkaoda Vinyl Market") shouldBe EventType.OTHER.name
    }

    @Test
    fun `extracts the promoter from a presents prefix`() {
        arkaodaPromoters("pre:sense pres. Volpe (Live)") shouldContainExactly listOf("pre:sense")
        arkaodaPromoters("MILK ME presents: Laura Krieg + Schulverweis at Arkaoda") shouldContainExactly
            listOf("MILK ME")
        arkaodaPromoters("MILK ME presents - Le Prince Harry - A Long Way Down Remixed") shouldContainExactly
            listOf("MILK ME")
        arkaodaPromoters("FLURFUNK presents Kiernan Laveaux") shouldContainExactly listOf("FLURFUNK")
    }

    @Test
    fun `finds no promoter without a presents marker`() {
        arkaodaPromoters("Signal To Noise: Vicente Yáñez, Kėkė Søl, Guro Kverndokk").shouldBeEmpty()
        // The marker must be its own word followed by whitespace, so a name starting
        // with the same letters is not read as "<promoter> pres…".
        arkaodaPromoters("Elvis Presley Tribute").shouldBeEmpty()
    }

    @Test
    fun `splits a comma-separated lineup after stripping the series prefix`() {
        arkaodaArtists("Signal To Noise: Vicente Yáñez, Kėkė Søl, Guro Kverndokk", concert)
            .map { it.name } shouldContainExactly listOf("Vicente Yáñez", "Kėkė Søl", "Guro Kverndokk")
        arkaodaArtists("Intursit (live), Poko Cox, Chikiss", concert)
            .map { it.name } shouldContainExactly listOf("Intursit", "Poko Cox", "Chikiss")
    }

    @Test
    fun `bills every act as headliner because the venue publishes no hierarchy`() {
        arkaodaArtists("Roomer + Clarice", concert).map { it.role } shouldContainExactly
            listOf("HEADLINER", "HEADLINER")
    }

    @Test
    fun `recovers the act from a promoter and venue framed title`() {
        arkaodaArtists("pre:sense pres. Volpe (Live)", concert).map { it.name } shouldContainExactly listOf("Volpe")
        arkaodaArtists("MILK ME presents: Laura Krieg + Schulverweis at Arkaoda", concert)
            .map { it.name } shouldContainExactly listOf("Laura Krieg", "Schulverweis")
    }

    @Test
    fun `keeps a country tag from fragmenting a lineup and drops it from the name`() {
        // The unpadded "/" inside "(PL/USA)" is not an act separator; the padded one before IFS is.
        arkaodaArtists("Chris Pitsiokos, Qba Janicki & Marta Warelis (PL/USA) / IFS meets AGF (PL/DE)", concert)
            .map { it.name } shouldContainExactly
            listOf("Chris Pitsiokos", "Qba Janicki", "Marta Warelis", "IFS meets AGF")
    }

    @Test
    fun `takes a single unadorned title as the act`() {
        arkaodaArtists("Juana Aguirre", concert).map { it.name } shouldContainExactly listOf("Juana Aguirre")
    }

    @Test
    fun `extracts no artists from a compound event label`() {
        arkaodaArtists("Osàre! Editions x arkaoda", concert).shouldBeEmpty()
        arkaodaArtists("AL.Berlin - Remise Takeover", concert).shouldBeEmpty()
        arkaodaArtists("Alonas FFS Fundraiser", concert).shouldBeEmpty()
        // Deliberately conservative: the real acts are lost with the release-party framing.
        arkaodaArtists("""Post Clients & Friends: 7" Vinyl Release Party""", concert).shouldBeEmpty()
        arkaodaArtists("Grumpy Pieces release; Harmonious Thelonious (Live) + Saeko Killy (Live)", concert)
            .shouldBeEmpty()
    }

    @Test
    fun `extracts no artists from an event the venue did not label a concert`() {
        arkaodaArtists("Bar Night: Bent (DJ)", EventType.OTHER.name).shouldBeEmpty()
        arkaodaArtists("Etikett Radio Family Party 001", EventType.PARTY.name).shouldBeEmpty()
    }
}
