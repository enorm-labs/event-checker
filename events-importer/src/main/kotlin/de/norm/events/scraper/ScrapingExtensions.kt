package de.norm.events.scraper

import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import java.net.URI

/**
 * Extracts trimmed text content from the first child element matching [cssQuery].
 *
 * Returns `null` if no element matches or the text is blank.
 * This is the most common extraction pattern in scrapers — wraps the
 * verbose `selectFirst(...)?.text()?.trim()?.takeIf { ... }` chain.
 *
 * Example:
 * ```kotlin
 * val title = content.textAt("h1.event-title")
 * val genre = item.textAt("[fs-cmsfilter-field=genre]")
 * ```
 */
fun Element.textAt(cssQuery: String): String? =
    selectFirst(cssQuery)
        ?.text()
        ?.trim()
        ?.takeIf { it.isNotBlank() }

/**
 * Extracts an attribute value from the first child element matching [cssQuery].
 *
 * Returns `null` if no element matches or the attribute value is blank.
 *
 * Example:
 * ```kotlin
 * val href = content.attrAt("a.ticket-link", "href")
 * val style = item.attrAt(".image-wrapper", "style")
 * ```
 */
fun Element.attrAt(
    cssQuery: String,
    attributeKey: String
): String? =
    selectFirst(cssQuery)
        ?.attr(attributeKey)
        ?.takeIf { it.isNotBlank() }

/**
 * Extracts an absolute image URL from the `src` attribute of the first
 * `<img>` element matching [cssQuery].
 *
 * Returns `null` if no element matches, the `src` is blank, or the URL
 * is not absolute (does not start with `http`). This filters out
 * placeholder values like empty strings or relative paths.
 *
 * Example:
 * ```kotlin
 * val imageUrl = content.imgSrcAt("img.eventpage-image")
 * ```
 */
fun Element.imgSrcAt(cssQuery: String): String? =
    attrAt(cssQuery, "src")
        ?.takeIf { it.startsWith("http") }

/**
 * Extracts an absolute URL from the `href` attribute of the first
 * anchor element matching [cssQuery].
 *
 * Returns `null` if no element matches, the `href` is blank, or the URL
 * is not absolute (does not start with `http`).
 *
 * Example:
 * ```kotlin
 * val ticketUrl = content.hrefAt("a.ticket-link")
 * ```
 */
fun Element.hrefAt(cssQuery: String): String? =
    attrAt(cssQuery, "href")
        ?.takeIf { it.startsWith("http") }

/**
 * Checks whether any child element matching [cssQuery] is visible in the
 * Webflow CMS conditional visibility system and contains the given [text].
 *
 * Webflow CMS uses the `w-condition-invisible` class to hide elements
 * that don't meet a CMS condition. This is commonly used for status flags
 * (e.g. "Sold-Out", "Cancelled") that are always present in the DOM but
 * conditionally shown based on a CMS toggle field.
 *
 * A flag is considered "visible" if it does **not** have the
 * `w-condition-invisible` class and its text content contains [text]
 * (case-insensitive).
 *
 * Example:
 * ```kotlin
 * val isSoldOut = item.hasVisibleWebflowFlag(".event-detail.sold-out", "Sold-Out")
 * val isCancelled = item.hasVisibleWebflowFlag(".event-detail.sold-out", "Cancelled")
 * ```
 */
fun Element.hasVisibleWebflowFlag(
    cssQuery: String,
    text: String
): Boolean =
    select(cssQuery)
        .any { !it.hasClass("w-condition-invisible") && it.text().contains(text, ignoreCase = true) }

/**
 * Splits the text under the first element matching [cssQuery] into its
 * `<br>`-delimited lines, each trimmed, with blank lines dropped.
 *
 * Reading a multi-line subtitle/blurb as discrete lines — rather than the
 * whitespace-flattened `.text()` — lets callers target a specific line (e.g. the
 * "Support:" line) without absorbing the others (e.g. a trailing cancellation
 * note appended on a later line). Only direct-child `<br>` elements break a line;
 * text inside nested inline elements is appended to the current line. Returns an
 * empty list when no element matches.
 *
 * Example:
 * ```kotlin
 * // subtitle = "+ Support: Jeff Clarke<br><br>ABGESAGT. …note…"
 * article.textLinesAt(".event__subtitle")  // ["+ Support: Jeff Clarke", "ABGESAGT. …note…"]
 * ```
 */
fun Element.textLinesAt(cssQuery: String): List<String> {
    val element = selectFirst(cssQuery) ?: return emptyList()
    val lines = mutableListOf<String>()
    val current = StringBuilder()
    for (node in element.childNodes()) {
        when {
            node is Element && node.tagName().equals("br", ignoreCase = true) -> {
                lines.add(current.toString())
                current.clear()
            }

            node is TextNode -> {
                current.append(node.text())
            }

            node is Element -> {
                current.append(node.text())
            }
        }
    }
    lines.add(current.toString())
    return lines.map { it.trim() }.filter { it.isNotBlank() }
}

/**
 * Resolves a potentially relative [href] against the [baseUrl].
 *
 * If the href is already absolute (starts with "http"), it is returned as-is.
 * Otherwise, it is resolved against the base URL using [URI.resolve].
 *
 * This is a common operation when scraping venue pages that use relative
 * links for event detail pages.
 *
 * Example:
 * ```kotlin
 * resolveUrl("https://venue.com/events", "/event/foo")  // "https://venue.com/event/foo"
 * resolveUrl("https://venue.com/events", "https://other.com/bar")  // "https://other.com/bar"
 * ```
 */
fun resolveUrl(
    baseUrl: String,
    href: String
): String {
    if (href.startsWith("http")) return href
    return URI(baseUrl).resolve(href).toString()
}

/**
 * Extracts the event slug from a Kulturhäuser-platform detail URL by stripping
 * the path [prefix] (default `/events/`) and any trailing slash.
 *
 * Example: `https://www.lido-berlin.de/events/2026-06-15-sorry` → `2026-06-15-sorry`.
 *
 * The slug is the stable URL identity even when its embedded date is stale (the
 * platform keeps the original slug when an event is rescheduled), so it is used
 * to build a stable `sourceId`. Shared by Astra and Lido.
 */
fun extractEventSlug(
    url: String,
    prefix: String = "/events/"
): String = URI(url).path.removePrefix(prefix).trimEnd('/')
