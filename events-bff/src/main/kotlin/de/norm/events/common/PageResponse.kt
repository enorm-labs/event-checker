package de.norm.events.common

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Pageable

/**
 * Generic paged response wrapper for list endpoints.
 *
 * The list endpoints return pagination metadata so the frontend can render page controls
 * (total counts, "page X of N") without an extra count request.
 *
 * The importer's admin API answers in the same envelope, and did not until #810 — where a bare array
 * let a script write 20 of 86 sources and report success. **The two copies must stay the same shape**,
 * because one API answering differently from the other is the asymmetry that caused it.
 */
@Schema(description = "A page of results with pagination metadata")
data class PageResponse<T>(
    @Schema(description = "The items on this page")
    val content: List<T>,
    @Schema(description = "Zero-based index of this page", example = "0")
    val page: Int,
    @Schema(description = "Requested page size", example = "20")
    val size: Int,
    @Schema(description = "Total number of matching items across all pages", example = "137")
    val totalElements: Long,
    @Schema(description = "Total number of pages", example = "7")
    val totalPages: Int
) {
    companion object {
        fun <T> of(
            content: List<T>,
            pageable: Pageable,
            totalElements: Long
        ): PageResponse<T> =
            PageResponse(
                content = content,
                page = pageable.pageNumber,
                size = pageable.pageSize,
                totalElements = totalElements,
                totalPages = if (pageable.pageSize == 0) 0 else ((totalElements + pageable.pageSize - 1) / pageable.pageSize).toInt()
            )
    }
}
