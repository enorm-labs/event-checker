package de.norm.events.common

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Pageable

/**
 * Generic paged response wrapper for list endpoints.
 *
 * Unlike the importer's admin API (which returns bare JSON arrays), the public BFF
 * list endpoints return pagination metadata so the frontend can render page controls
 * (total counts, "page X of N") without an extra count request.
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
        /** Builds a [PageResponse] from the page [content], the requesting [pageable], and the overall [totalElements] count. */
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
