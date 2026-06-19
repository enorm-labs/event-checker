package de.norm.events.genretag

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read service exposing the genre tag list for frontend filtering.
 */
@Service
class GenreTagService(
    private val genreTagRepository: GenreTagRepository
) {
    /** Returns all genre tags, sorted alphabetically by name. */
    @Transactional(readOnly = true)
    suspend fun list(): List<GenreTagResponse> = genreTagRepository.findAllByOrderByName().map { GenreTagResponse.fromEntity(it) }.toList()
}
