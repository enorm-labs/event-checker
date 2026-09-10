package de.norm.events.image

/**
 * The credit a fixture image carries, as SQL fragments.
 *
 * V020 refuses a venue, artist or promoter row that has an `image_url` and no attribution, so every
 * fixture that gives one of them an image supplies a credit with it.
 */
internal const val IMAGE_CREDIT_SET =
    "image_attribution = 'Fixture Photographer', image_licence_id = 'CC-BY-SA-4.0', " +
        "image_source_url = 'https://commons.wikimedia.org/wiki/File:Fixture.jpg'"

/** @see IMAGE_CREDIT_SET */
internal const val IMAGE_CREDIT_COLUMNS = "image_attribution, image_licence_id, image_source_url"

/** @see IMAGE_CREDIT_SET */
internal const val IMAGE_CREDIT_VALUES =
    "'Fixture Photographer', 'CC-BY-SA-4.0', 'https://commons.wikimedia.org/wiki/File:Fixture.jpg'"
