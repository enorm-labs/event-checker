package de.norm.events.scraper.binuu

import de.norm.events.scraper.parseTime
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Document
import tools.jackson.core.json.JsonReadFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

/** Base URL for Bi Nuu's PocketBase file store; event `image.url` values are stored relative to it. */
internal const val BINUU_IMAGE_BASE_URL = "https://pb.binuu.de/api/files/"

/**
 * Extracts the SvelteKit SSR data payload embedded in Bi Nuu pages.
 *
 * Bi Nuu runs on SvelteKit backed by PocketBase. Every page inlines its route
 * data as a JavaScript object literal inside the `kit.start(...)` bootstrap
 * `<script>` — the listing carries `…data:{events:[…]}` and a detail page
 * `…data:{item:{…}}`. This embedded payload is the most stable source on the
 * site: it is structured, machine-readable, survives visual redesigns, and —
 * unlike the rendered DOM — carries full ISO dates *with the year* (the visible
 * cards only show `Sa 11.07.`).
 *
 * The literal uses **unquoted property names**, so it is parsed with a lenient
 * Jackson mapper rather than strict JSON. Braces routinely appear inside string
 * values (blurhash previews like `"KPIi{^Av…"`), so the object boundary is found
 * with a string-aware brace scan, never a naive `}` search.
 */
internal object BinuuSvelteKitPayload {
    private val logger = KotlinLogging.logger {}

    private val lenientJson: JsonMapper =
        JsonMapper
            .builder()
            .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .build()

    /**
     * Parses the object literal wrapping [marker] from the page's SvelteKit
     * bootstrap script and returns the node at [key], or `null` if it is absent
     * or unparseable.
     *
     * @param document the parsed Jsoup document of the listing or detail page.
     * @param marker the key + opening bracket anchoring the wrapping object,
     *   e.g. `"events:["` for the listing or `"item:{"` for a detail page.
     * @param key the property to return from the parsed object (`"events"` / `"item"`).
     */
    @Suppress(
        "TooGenericExceptionCaught", // A malformed/absent payload must degrade to null, never abort the import
        "ReturnCount" // Guard clauses for the missing script and unparseable payload are clearer than nesting
    )
    fun dataNode(
        document: Document,
        marker: String,
        key: String
    ): JsonNode? {
        val script = document.select("script").map { it.data() }.firstOrNull { it.contains(marker) }
        if (script == null) {
            logger.warn { "No Bi Nuu SvelteKit bootstrap script containing '$marker' found" }
            return null
        }
        val json = extractObjectLiteral(script, marker) ?: return null
        return try {
            lenientJson.readTree(json).get(key)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to parse Bi Nuu SvelteKit '$key' payload" }
            null
        }
    }

    /** Extracts the balanced `{…}` object that opens immediately before [marker]. */
    @Suppress("ReturnCount") // Sequential null-guards for each extraction step are clearer than nesting
    private fun extractObjectLiteral(
        script: String,
        marker: String
    ): String? {
        val markerIndex = script.indexOf(marker)
        if (markerIndex < 0) return null
        val objectStart = script.lastIndexOf('{', markerIndex)
        if (objectStart < 0) return null
        val objectEnd = matchClosingBrace(script, objectStart)
        if (objectEnd < 0) return null
        return script.substring(objectStart, objectEnd + 1)
    }

    /**
     * Returns the index of the `}` closing the `{` at [start], skipping any braces
     * that appear inside double-quoted strings (blurhash previews embed literal `{`).
     * Returns -1 if the object is never closed.
     */
    private fun matchClosingBrace(
        text: String,
        start: Int
    ): Int {
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until text.length) {
            val c = text[i]
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
            } else {
                when (c) {
                    '"' -> {
                        inString = true
                    }

                    '{' -> {
                        depth++
                    }

                    '}' -> {
                        depth--
                        if (depth == 0) return i
                    }
                }
            }
        }
        return -1
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

/** Reads the string array at [field] as a list of trimmed, non-blank values. */
internal fun JsonNode.stringList(field: String): List<String> =
    path(field).mapNotNull { element ->
        element
            .takeUnless { it.isNull }
            ?.asString()
            ?.trim()
            ?.takeIf(String::isNotBlank)
    }

/**
 * Builds the absolute image URL for an event/promoter node from its nested
 * `image.url`, prefixing the PocketBase file base for relative paths and passing
 * already-absolute URLs through unchanged. Returns `null` when no image is present.
 */
internal fun JsonNode.binuuImageUrl(): String? {
    val url = path("image").stringOrNull("url") ?: return null
    return if (url.startsWith("http")) url else BINUU_IMAGE_BASE_URL + url
}

/**
 * Parses the date from a Bi Nuu timestamp like `"2026-07-19 19:00:00.000Z"`.
 *
 * The trailing `Z` is spurious — the values are local Berlin wall-clock times
 * (a 19:00 concert is stored as `19:00…Z`, not converted to UTC), so only the
 * leading `yyyy-MM-dd` is read and no timezone shift is applied. Returns `null`
 * for missing or unparseable input.
 */
internal fun parseBinuuDate(raw: String?): LocalDate? {
    if (raw.isNullOrBlank() || raw.length < DATE_LENGTH) return null
    return try {
        LocalDate.parse(raw.trim().substring(0, DATE_LENGTH))
    } catch (_: DateTimeParseException) {
        null
    }
}

/**
 * Parses the `HH:mm` time from a Bi Nuu timestamp like `"2026-07-19 19:00:00.000Z"`,
 * reading the wall-clock time after the space (see [parseBinuuDate] on the spurious
 * `Z`). Returns `null` when there is no time component or it is unparseable.
 */
internal fun parseBinuuTime(raw: String?): LocalTime? {
    val timePart = raw?.trim()?.substringAfter(' ', "")?.take(HH_MM_LENGTH)
    return parseTime(timePart?.takeIf { it.isNotBlank() })
}

/**
 * Maps Bi Nuu's single-letter `eventStatus` code to a domain status name.
 *
 * Observed codes: `"r"` (Verlegt / relocated, carries `locationNew`) and `"p"`
 * (Verschoben / postponed, carries the original date in `startOld`). Any other
 * non-blank code is logged and treated as [SCHEDULED][de.norm.events.event.EventStatus.SCHEDULED]
 * so a new code surfaces in the logs rather than being silently mismapped.
 */
internal fun mapBinuuStatus(code: String?): String {
    val logger = KotlinLogging.logger("de.norm.events.scraper.binuu.BinuuStatus")
    return when (val normalized = code?.trim()?.lowercase()) {
        null, "" -> {
            "SCHEDULED"
        }

        "r" -> {
            "RELOCATED"
        }

        "p" -> {
            "POSTPONED"
        }

        else -> {
            logger.warn { "Unknown Bi Nuu eventStatus code '$normalized', defaulting to SCHEDULED" }
            "SCHEDULED"
        }
    }
}

private const val DATE_LENGTH = 10
private const val HH_MM_LENGTH = 5
