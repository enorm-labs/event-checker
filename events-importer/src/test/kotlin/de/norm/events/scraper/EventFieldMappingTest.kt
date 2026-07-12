package de.norm.events.scraper

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalTime

// Focused tests for EventFieldMapping — one test per behaviour.
class EventFieldMappingTest {
    // --- cleanEventTitle ---

    @Test
    fun `cleanEventTitle strips a trailing reschedule note and stray dash`() {
        cleanEventTitle("Iggi Kelly Nachholtermin vom 28.04.26-") shouldBe "Iggi Kelly"
        cleanEventTitle("The Dear Hunter -Nachholtermin vom 30.09.2025.") shouldBe "The Dear Hunter"
        cleanEventTitle("Some Show -") shouldBe "Some Show"
    }

    @Test
    fun `cleanEventTitle strips a trailing sold-out annotation`() {
        // The status suffix is dropped so "… (ausverkauft)" and its non-sold-out twin
        // collapse to the same title (and the same title-derived headliner artist).
        cleanEventTitle("Singalong -Das große Mitsing-Event (ausverkauft)") shouldBe "Singalong -Das große Mitsing-Event"
        cleanEventTitle("Singalong -Das große Mitsing-Event") shouldBe "Singalong -Das große Mitsing-Event"
        cleanEventTitle("Some Show ausverkauft") shouldBe "Some Show"
        cleanEventTitle("Some Show - AUSVERKAUFT!") shouldBe "Some Show"
    }

    @Test
    fun `cleanEventTitle leaves a clean title and mid-title dash untouched`() {
        cleanEventTitle("Freshlyground") shouldBe "Freshlyground"
        cleanEventTitle("Tannz im Frannz -auf 2 Floors") shouldBe "Tannz im Frannz -auf 2 Floors"
        // "ausverkauft" only mid-title (never a real case, but proves the end-anchor) is kept.
        cleanEventTitle("Ausverkauft Tour Show") shouldBe "Ausverkauft Tour Show"
    }

    // --- detectFree ---

    @Test
    fun `detectFree is true for an explicit zero presale or box-office price`() {
        detectFree(pricePresale = BigDecimal.ZERO) shouldBe true
        detectFree(pricePresale = BigDecimal("0.00")) shouldBe true
        detectFree(priceBoxOffice = BigDecimal("0.00")) shouldBe true
    }

    @Test
    fun `detectFree is true for free-entry phrases in the price note or title`() {
        detectFree(priceNote = "Eintritt frei") shouldBe true
        detectFree(priceNote = "Freier Eintritt, Spende erwünscht") shouldBe true
        detectFree(priceNote = "Free entry all night") shouldBe true
        detectFree(title = "Sommerfest — Free Admission") shouldBe true
    }

    @Test
    fun `detectFree matches single-word markers only in the price note`() {
        detectFree(priceNote = "Gratis") shouldBe true
        detectFree(priceNote = "kostenlos") shouldBe true
        detectFree(priceNote = "umsonst") shouldBe true
        // A bare token in the title (not the pricing-scoped note) must not trigger.
        detectFree(title = "Gratis Vibes Live") shouldBe false
    }

    @Test
    fun `detectFree does not false-positive on names or word fragments`() {
        detectFree(title = "Freedom Festival") shouldBe false
        detectFree(title = "Freikörperkultur") shouldBe false
        detectFree(priceNote = "freestyle session") shouldBe false
        detectFree(pricePresale = BigDecimal("12.00"), priceBoxOffice = BigDecimal("15.00")) shouldBe false
    }

    @Test
    fun `detectFree is false when nothing is provided`() {
        detectFree() shouldBe false
        detectFree(priceNote = null, title = null) shouldBe false
    }

    // --- orderDoorsBeforeStart ---

    @Test
    fun `orderDoorsBeforeStart swaps a transposed doors-after-start pair`() {
        // SO36's "Einlass: 19:30, Beginn: 19:00" — labels swapped at the source.
        orderDoorsBeforeStart(LocalTime.of(19, 30), LocalTime.of(19, 0)) shouldBe
            (LocalTime.of(19, 0) to LocalTime.of(19, 30))
    }

    @Test
    fun `orderDoorsBeforeStart leaves an already-valid pair unchanged`() {
        orderDoorsBeforeStart(LocalTime.of(19, 0), LocalTime.of(20, 0)) shouldBe
            (LocalTime.of(19, 0) to LocalTime.of(20, 0))
    }

    @Test
    fun `orderDoorsBeforeStart leaves equal times unchanged`() {
        orderDoorsBeforeStart(LocalTime.of(20, 0), LocalTime.of(20, 0)) shouldBe
            (LocalTime.of(20, 0) to LocalTime.of(20, 0))
    }

    @Test
    fun `orderDoorsBeforeStart does not reorder when a time is missing`() {
        orderDoorsBeforeStart(null, LocalTime.of(20, 0)) shouldBe (null to LocalTime.of(20, 0))
        orderDoorsBeforeStart(LocalTime.of(19, 0), null) shouldBe (LocalTime.of(19, 0) to null)
        orderDoorsBeforeStart(null, null) shouldBe (null to null)
    }
}
