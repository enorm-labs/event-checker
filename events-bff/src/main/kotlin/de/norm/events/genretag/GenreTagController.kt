package de.norm.events.genretag

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Public read API exposing the genre tag list for the frontend event filter.
 */
@RestController
@RequestMapping("/genres")
@Tag(name = "Genres", description = "Public endpoint listing genre tags for filtering")
class GenreTagController(
    private val genreTagService: GenreTagService
) {
    @GetMapping
    @Operation(summary = "List all genre tags alphabetically")
    suspend fun list(): List<GenreTagResponse> = genreTagService.list()
}
