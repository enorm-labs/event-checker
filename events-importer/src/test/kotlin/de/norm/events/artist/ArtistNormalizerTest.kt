package de.norm.events.artist

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

/**
 * Unit tests for [canonicalArtistName].
 *
 * The cases are drawn from real artist names seen across the venue importers,
 * where the same act appears ALL-CAPS on one site and mixed case on another.
 */
class ArtistNormalizerTest {
    @Test
    fun `de-shouts all-caps act names to a clean display form`() {
        assertAll(
            { canonicalArtistName("GREEN LUNG") shouldBe "Green Lung" },
            { canonicalArtistName("MUNA") shouldBe "Muna" },
            { canonicalArtistName("THE BUTLERS") shouldBe "The Butlers" },
            { canonicalArtistName("EL FLECHA NEGRA") shouldBe "El Flecha Negra" }
        )
    }

    @Test
    fun `preserves intentional mixed casing`() {
        assertAll(
            { canonicalArtistName("Green Lung") shouldBe "Green Lung" },
            { canonicalArtistName("GreyZone") shouldBe "GreyZone" },
            { canonicalArtistName("will.i.am") shouldBe "will.i.am" },
            { canonicalArtistName("El Flecha Negra") shouldBe "El Flecha Negra" }
        )
    }

    @Test
    fun `keeps short all-caps tokens as acronyms`() {
        assertAll(
            { canonicalArtistName("DJ KOZE") shouldBe "DJ Koze" },
            { canonicalArtistName("MC SOLAAR") shouldBe "MC Solaar" },
            { canonicalArtistName("UK SUBS") shouldBe "UK Subs" },
            // Act names that are themselves initialisms.
            { canonicalArtistName("FKJ") shouldBe "FKJ" },
            { canonicalArtistName("AZ") shouldBe "AZ" },
            { canonicalArtistName("DBG") shouldBe "DBG" },
            // Short all-caps tokens with digits are acronym-like too.
            { canonicalArtistName("MC5") shouldBe "MC5" },
            { canonicalArtistName("UB40") shouldBe "UB40" }
        )
    }

    @Test
    fun `keeps a standalone two-letter all-caps name verbatim rather than title-casing it`() {
        assertAll(
            // A single short all-caps name is an initialism/stylisation, not a shouted word.
            { canonicalArtistName("JJ") shouldBe "JJ" },
            { canonicalArtistName("EV") shouldBe "EV" },
            { canonicalArtistName("YU") shouldBe "YU" },
            { canonicalArtistName("MØ") shouldBe "MØ" },
            // Scoped to the whole name: a short word inside a multi-word name still de-shouts.
            { canonicalArtistName("WARS OF ATTRITION") shouldBe "Wars Of Attrition" },
            // Three-plus letters stay a title-cased shouted word (accepted residual).
            { canonicalArtistName("MUNA") shouldBe "Muna" }
        )
    }

    @Test
    fun `leaves stylised tokens with digits, dots or slashes untouched`() {
        assertAll(
            { canonicalArtistName("AC/DC") shouldBe "AC/DC" },
            { canonicalArtistName("R.E.M.") shouldBe "R.E.M." },
            { canonicalArtistName("H2O") shouldBe "H2O" },
            { canonicalArtistName("HGICH.T") shouldBe "HGICH.T" }
        )
    }

    @Test
    fun `de-shouts words with attached punctuation, keeping the punctuation in place`() {
        assertAll(
            // Possessives and contractions — the apostrophe must not freeze the word in caps.
            { canonicalArtistName("MURPHY'S LAW") shouldBe "Murphy's Law" },
            { canonicalArtistName("FLAMIN' GROOVIES") shouldBe "Flamin' Groovies" },
            // Trailing/leading punctuation, commas and parentheses.
            { canonicalArtistName("AGATHA IS DEAD!") shouldBe "Agatha Is Dead!" },
            { canonicalArtistName("SICKBOYRARI (BLACK KRAY)") shouldBe "Sickboyrari (Black Kray)" },
            { canonicalArtistName("EARTH, WIND & FIRE") shouldBe "Earth, Wind & Fire" }
        )
    }

    @Test
    fun `never strips a word from a band name`() {
        assertAll(
            // Words a promoter normalizer would strip are load-bearing in band names.
            { canonicalArtistName("THE RECORDS") shouldBe "The Records" },
            { canonicalArtistName("ARCADE FIRE CONCERTS") shouldBe "Arcade Fire Concerts" }
        )
    }

    @Test
    fun `normalizes surrounding and internal whitespace`() {
        canonicalArtistName("  GREEN   LUNG  ") shouldBe "Green Lung"
    }

    @Test
    fun `falls back to the trimmed input for blank names`() {
        canonicalArtistName("   ") shouldBe ""
    }
}
