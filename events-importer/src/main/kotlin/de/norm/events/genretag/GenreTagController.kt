package de.norm.events.genretag

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Read-only REST controller for genre tags.
 *
 * Genre tags are auto-created during event imports — there is no manual
 * create/update/delete API. This controller exposes the available tags
 * so the frontend can populate filter dropdowns and link to filtered views.
 */
@RestController
@RequestMapping("/api/admin/genre-tags")
@Tag(name = "Admin: Genre Tags", description = "Read-only endpoints for normalized genre tags")
class GenreTagController(
    private val genreTagService: GenreTagService
) {
    @GetMapping
    @Operation(summary = "List all genre tags with pagination")
    suspend fun findAll(
        @PageableDefault(size = 100, sort = ["name"]) pageable: Pageable
    ): List<GenreTagResponse> = genreTagService.findAll(pageable)

    @GetMapping("/{id}")
    @Operation(summary = "Get a single genre tag by ID")
    suspend fun findById(
        @PathVariable id: Long
    ): GenreTagResponse = genreTagService.findById(id)
}
