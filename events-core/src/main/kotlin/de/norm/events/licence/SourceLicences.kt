package de.norm.events.licence

/**
 * What one source permits, for the two fields that need permission.
 *
 * `null` means nobody reviewed that field, which is not the same as
 * [SourceLicence.UNCLEAR] and displays for the same reason.
 *
 * **This lives in core because two modules now ask the same question.** The BFF withholds a
 * prohibited field from a response, and the importer declines to store one at all (#807). One rule
 * in one place is what keeps those two answers from drifting apart.
 */
data class SourceLicences(
    val description: SourceLicence?,
    val image: SourceLicence?,
    val translation: SourceLicence? = null
) {
    /**
     * Whether the description must be withheld.
     *
     * **Only [SourceLicence.PROHIBITED] withholds.** `UNCLEAR` and `null` both display. That is the
     * decision taken on #283 and it is deliberately fail-open: silence from a venue is not a
     * refusal, and blanking every unreviewed source would remove material nobody objected to.
     *
     * `docs/SCRAPING_POSITION.md` §3.1 records what this accepts. The test that pins every branch
     * of this rule is what makes flipping it a decision rather than a tidy-up.
     */
    fun withholdsDescription(): Boolean = description == SourceLicence.PROHIBITED

    /** The same rule for images, answered from the source's own column. */
    fun withholdsImage(): Boolean = image == SourceLicence.PROHIBITED

    /**
     * Whether we may machine-translate this source's descriptions.
     *
     * **Only [SourceLicence.PERMITTED] allows it, which is the opposite of the display rule above.**
     * A translation is an adaptation under § 23 UrhG and needs the author's consent, so silence
     * cannot be read as permission the way it is for the display of the text as published (ADR-026).
     * `docs/SCRAPING_POSITION.md` §3.1 carries the reasoning, and #808 is how a source reaches
     * `PERMITTED`.
     */
    fun allowsTranslation(): Boolean = translation == SourceLicence.PERMITTED

    companion object {
        /**
         * What an event with no source at all permits.
         *
         * `event.event_source_id` is nullable and `ON DELETE SET NULL`, so an event outlives the
         * source that produced it. Such a row has no prohibition attached to it and therefore
         * displays, which is the same answer fail-open gives everywhere else.
         */
        val UNKNOWN_SOURCE = SourceLicences(description = null, image = null, translation = null)

        /** Reads the three columns of a source row, treating anything unrecognised as [SourceLicence.PROHIBITED]. */
        fun of(
            descriptionLicence: String?,
            imageLicence: String?,
            translationLicence: String? = null
        ): SourceLicences =
            SourceLicences(
                description = descriptionLicence?.let(SourceLicence::parseOrProhibited),
                image = imageLicence?.let(SourceLicence::parseOrProhibited),
                translation = translationLicence?.let(SourceLicence::parseOrProhibited)
            )
    }
}
