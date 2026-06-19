package de.norm.events.common

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

/**
 * Returns a copy of this [Pageable] whose sort is restricted to the [allowed] property names,
 * falling back to [default] when nothing valid remains.
 *
 * Spring Data R2DBC derived queries embed the sort property directly into the generated SQL.
 * Passing a raw client-supplied [Pageable] through therefore lets the caller drive `ORDER BY`:
 * unknown-but-clean properties reach the database as-is, and properties containing characters
 * outside Spring Data's safe set (e.g. Swagger UI's `["string"]` placeholder) trip
 * `SqlSort.validate`, surfacing as an unhandled `IllegalArgumentException` → 500.
 *
 * Whitelisting here (mirroring the events search's `ORDER BY` allowlist) keeps sorting
 * deterministic and injection-safe: orders whose property is not in [allowed] are dropped.
 */
fun Pageable.sanitizeSort(
    allowed: Set<String>,
    default: Sort
): Pageable {
    val safeOrders = sort.toList().filter { it.property in allowed }
    val safeSort = if (safeOrders.isEmpty()) default else Sort.by(safeOrders)
    return PageRequest.of(pageNumber, pageSize, safeSort)
}
