package de.norm.events.scraper.velomax

import de.norm.events.scraper.EventSource

/**
 * The three halls whose programmes share the one Velomax listing page.
 *
 * `velomax.de/events` interleaves the events of all three halls in one chronological run. Unlike
 * Club der Visionäre's colour-coded rooms, the hall here is stated three times over — as a CSS
 * class on the entry, as a `.location` label, and by the domain its detail link points at — so
 * [cssClass] is a durable filter rather than a last resort.
 *
 * Each hall is a separate import source with its own venue and `sourceId` prefix, all served by
 * [VelomaxOverviewPageScraper] filtering on that class.
 */
enum class VelomaxHall(
    /** The CSS class the listing puts on an entry belonging to this hall. */
    val cssClass: String,
    /** The import source this hall's events are attributed to. */
    val eventSource: EventSource
) {
    /** The Prenzlauer Berg arena — the largest of the three, and the one with most of the sport. */
    MAX_SCHMELING_HALLE("msh", EventSource.MAX_SCHMELING_HALLE),

    /** The Landsberger Allee arena. */
    VELODROM("velodrom", EventSource.VELODROM),

    /** The smaller hall configured inside the Velodrom, listed under its own `UFO` location. */
    UFO_IM_VELODROM("ufo", EventSource.UFO_IM_VELODROM)
}
