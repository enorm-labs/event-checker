package de.norm.events.event

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * The classifier is a stop-word count, so its worth is empirical rather than structural.
 *
 * It was measured against the 2,147 stored production descriptions and 60 of them labelled by
 * hand: 59 agreed, and the one disagreement called a nine-word German note `de` where the label
 * said "too short to judge". **No text was given the wrong language.** That is the error the page
 * cannot survive, because a wrong `lang` misleads a screen reader and a crawler, while a null one
 * only declines to say. The cases below are the shapes that measurement found.
 */
class DescriptionLanguageTest {
    @Test
    @DisplayName("German prose is German")
    fun `detects german`() {
        val detected = DescriptionLanguage.detect(GERMAN_DESCRIPTION)

        detected?.language shouldBe DescriptionLanguage.GERMAN
        (detected?.confidence?.compareTo(BigDecimal("0.8")) ?: -1) shouldBe 1
    }

    @Test
    @DisplayName("English prose is English")
    fun `detects english`() {
        DescriptionLanguage.detect(ENGLISH_DESCRIPTION)?.language shouldBe DescriptionLanguage.ENGLISH
    }

    // Klunkerkranich writes the German text, a literal [EN] marker, then the English one. Splitting
    // that field is #330's work. Until then the honest answer is that the field has no one language.
    @Test
    @DisplayName("one field holding both languages is unknown")
    fun `declines a bilingual field`() {
        DescriptionLanguage.detect("$GERMAN_DESCRIPTION\n[EN]\n$ENGLISH_DESCRIPTION").shouldBeNull()
    }

    @Test
    @DisplayName("a two-line note is unknown")
    fun `declines text below the word floor`() {
        DescriptionLanguage.detect("Doors: 19:30 Start: 20:00").shouldBeNull()
    }

    // A line-up is a list of names. It carries no grammar to count, whatever language it is in.
    @Test
    @DisplayName("a line-up list is unknown")
    fun `declines a list of names`() {
        DescriptionLanguage.detect("HOPPER\nAlessia Ceruti\nAlexkid\nMagda\nTripmastaz\nDJ Gee36\nMC Caramel").shouldBeNull()
    }

    // Lido and Columbia Theater both publish Spanish biographies. Neither of our two answers is
    // right for those, and guessing English because "the" appears twice is worse than saying nothing.
    @Test
    @DisplayName("a third language is unknown rather than the nearer of the two")
    fun `declines a third language`() {
        val spanish =
            "Perotá Chingó es un dúo argentino que ha conquistado audiencias de todo el mundo con su propuesta de folk " +
                "latinoamericano, armonías vocales y una sensibilidad que atraviesa el continente."

        DescriptionLanguage.detect(spanish).shouldBeNull()
    }

    @Test
    @DisplayName("no description has no language")
    fun `declines null`() {
        DescriptionLanguage.detect(null).shouldBeNull()
    }

    // The hash is what tells a translation that its original moved on, so it has to change with it.
    @Test
    @DisplayName("the hash follows the text")
    fun `hashes the text`() {
        DescriptionLanguage.hash("a") shouldBe DescriptionLanguage.hash("a")
        (DescriptionLanguage.hash("a") == DescriptionLanguage.hash("b")) shouldBe false
        DescriptionLanguage.hash("a").length shouldBe 64
    }

    private companion object {
        const val GERMAN_DESCRIPTION =
            "Die Bolschewistische Kurkapelle Schwarz-Rot wurde 1986 in Ost-Berlin als Teil der gegenkulturellen und " +
                "politischen Untergrundszene gegründet, wenige Jahre vor dem Fall der Berliner Mauer."
        const val ENGLISH_DESCRIPTION =
            "Tara Nome Doyle is a Berlin-based singer-songwriter and producer with Norwegian-Irish roots. Over three " +
                "albums and a widening web of collaborations, she has made a sound that is entirely her own."
    }
}
