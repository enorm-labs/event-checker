package de.norm.events.scraper.loge

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Document
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/**
 * Extracts the Wix Events data payload embedded in Loge's `/event-list` page.
 *
 * Loge runs on Wix with a Wix Events widget. Although the widget renders
 * client-side, Wix server-side-injects the full event data as strict JSON in a
 * `<script type="application/json" id="wix-warmup-data">` block — a stable,
 * machine-readable source (ADR-007 §"Prefer a JSON / API Source") that survives
 * visual redesigns and carries clean ISO timestamps rather than the German
 * date text (`17. Juli 2026`) shown in the rendered cards.
 *
 * The events live at
 * `appsWarmupData → <Wix Events appDefId> → <widget key> → events.events`. The
 * app-definition id [WIX_EVENTS_APP_DEF_ID] is a global Wix constant (identical
 * across every Wix site), while the widget key (`widgetTPASection_…`) is
 * per-page, so the widget node is located by looking for the child that actually
 * carries an `events.events` array rather than hard-coding the key.
 */
internal object LogeWixWarmupData {
    private val logger = KotlinLogging.logger {}

    /** The Wix Events app definition id — a global Wix constant shared by every Wix site. */
    private const val WIX_EVENTS_APP_DEF_ID = "140603ad-af8d-84a5-2c80-a0f60cb47351"

    /** Id of the `<script>` element Wix injects its server-side warmup state into. */
    private const val WARMUP_SCRIPT_ID = "wix-warmup-data"

    private val jsonMapper: JsonMapper = JsonMapper.builder().build()

    /**
     * Returns the `events.events` array node from the page's warmup data, or
     * `null` when the warmup script is absent, unparseable, or carries no Wix
     * Events widget (e.g. an empty program).
     *
     * @param document the parsed Jsoup document of the `/event-list` overview page.
     */
    @Suppress(
        "TooGenericExceptionCaught", // A malformed/absent payload must degrade to null, never abort the import
        "ReturnCount" // Sequential null-guards for each extraction step are clearer than nesting
    )
    fun events(document: Document): JsonNode? {
        val script = document.getElementById(WARMUP_SCRIPT_ID)
        if (script == null) {
            logger.warn { "No '$WARMUP_SCRIPT_ID' script found on Loge overview page" }
            return null
        }
        val root =
            try {
                jsonMapper.readTree(script.data())
            } catch (e: Exception) {
                logger.warn(e) { "Failed to parse Loge wix-warmup-data JSON" }
                return null
            }
        val appNode = root.path("appsWarmupData").path(WIX_EVENTS_APP_DEF_ID)
        if (!appNode.isObject) {
            logger.warn { "No Wix Events app data in Loge warmup payload" }
            return null
        }
        // The widget key (widgetTPASection_…) is per-page, so find the child that carries the events
        // array. Iterating a JsonNode object yields its property values (same as an array yields elements).
        val events = appNode.firstNotNullOfOrNull { widget -> widget.path("events").path("events").takeIf { it.isArray } }
        if (events == null) {
            logger.warn { "No events array in Loge Wix Events warmup payload" }
        }
        return events
    }
}

/**
 * Reads a trimmed string [field] from this node, or `null` when the field is
 * missing, JSON `null`, or blank.
 */
internal fun JsonNode.stringOrNull(field: String): String? {
    val node = path(field)
    if (node.isMissingNode || node.isNull) return null
    return node.asString().trim().takeIf { it.isNotBlank() }
}

/**
 * Parses the Berlin-local date and start time from a Wix `scheduling.config`
 * node. The `startDate` is a UTC instant (`2026-07-17T17:00:00.000Z`) paired
 * with a `timeZoneId` (`Europe/Berlin`), so it is converted to that zone to
 * recover the wall-clock date/time (19:00, not the 17:00 UTC value). Falls back
 * to [FALLBACK_ZONE] when the zone is absent or unknown. Returns `(null, null)`
 * for a missing/unparseable `startDate` or a to-be-decided (`scheduleTbd`) event.
 */
@Suppress("ReturnCount") // Guard clauses for the missing/unparseable startDate are clearer than nesting
internal fun parseLogeSchedule(config: JsonNode): Pair<LocalDate?, LocalTime?> {
    val startDate = config.stringOrNull("startDate") ?: return null to null
    val instant =
        try {
            Instant.parse(startDate)
        } catch (_: DateTimeParseException) {
            return null to null
        }
    val zone =
        config.stringOrNull("timeZoneId")?.let { id ->
            runCatching { ZoneId.of(id) }.getOrNull()
        } ?: FALLBACK_ZONE
    val zoned = instant.atZone(zone)
    return zoned.toLocalDate() to zoned.toLocalTime()
}

/** Default zone for Loge schedules missing a usable `timeZoneId` — every scraped venue is in Berlin. */
private val FALLBACK_ZONE: ZoneId = ZoneId.of("Europe/Berlin")
