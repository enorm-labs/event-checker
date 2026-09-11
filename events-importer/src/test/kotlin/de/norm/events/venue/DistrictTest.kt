package de.norm.events.venue

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DistrictTest {
    @Test
    fun `every district round-trips through its wire form`() {
        District.entries.forEach { District.fromValue(it.value) shouldBe it }
    }

    @Test
    fun `all 23 pre-2001 districts are present`() {
        District.entries.size shouldBe 23
    }

    @Nested
    inner class Rejections {
        // The level the column held before #1307. Two of the twelve are ambiguous under the new set
        // (`mitte`, `pankow` keep their spelling), so the merged name is the one that has to fail loudly.
        @Test
        fun `a borough is not a district and is refused`() {
            shouldThrow<IllegalArgumentException> { District.fromValue("friedrichshain-kreuzberg") }
        }

        // The other direction of the same mistake: finer than a district. Moabit is Tiergarten here.
        @Test
        fun `an Ortsteil is not a district and is refused`() {
            shouldThrow<IllegalArgumentException> { District.fromValue("moabit") }
        }

        @Test
        fun `the refusal names what was expected, so the caller can fix it`() {
            val message = shouldThrow<IllegalArgumentException> { District.fromValue("xberg") }.message!!

            message.contains("xberg") shouldBe true
            message.contains("kreuzberg") shouldBe true
        }

        // There is deliberately no lenient fallback. A wrong district moves a pin or removes a venue
        // from a radius search, and both are worse than refusing the write.
        @Test
        fun `an unknown value throws rather than falling back to a default`() {
            shouldThrow<IllegalArgumentException> { District.fromValue("bezirk-unbekannt") }
        }
    }

    @Nested
    inner class Parsing {
        @Test
        fun `surrounding whitespace does not change the answer`() {
            District.fromValue("  mitte  ") shouldBe District.MITTE
        }

        @Test
        fun `case does not change the answer`() {
            District.fromValue("Prenzlauer-Berg") shouldBe District.PRENZLAUER_BERG
        }
    }
}
