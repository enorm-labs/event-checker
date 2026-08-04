package de.norm.events.scraper.clubdervisionaere

import de.norm.events.scraper.EventSource

/**
 * The three rooms whose programmes share the one Club der Visionäre listing page.
 *
 * The page (`clubdervisionaere.com/programm/`, mirrored byte-identically at
 * `hoppetosse.berlin/program/`) interleaves the nights of all three rooms in one
 * chronological run. The **only** marker of which room a night belongs to is the
 * colour class the theme puts on its title paragraph — there is no per-room page,
 * category or data attribute, and the WordPress REST API exposes no room either.
 *
 * Each room is therefore a separate import source with its own venue and its own
 * `sourceId` prefix, all served by [ClubDerVisionaereProgrammePageScraper] filtering
 * on [titleClass].
 */
enum class ClubDerVisionaereRoom(
    /** The CSS class the theme puts on a title paragraph belonging to this room. */
    val titleClass: String,
    /** The import source this room's events are attributed to. */
    val eventSource: EventSource
) {
    /** The open-air club on the Flutgraben — the summer programme. */
    CLUB("cdvRed", EventSource.CLUB_DER_VISIONAERE),

    /** The indoor concert space next door, programmed year-round. */
    SONNENRAUM("sonnenraumYellow", EventSource.SONNENRAUM),

    /** The moored salon boat — the winter location. */
    MS_HOPPETOSSE("hoppetosseYellow", EventSource.MS_HOPPETOSSE)
}
