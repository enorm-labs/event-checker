package de.norm.events

import de.norm.events.artist.ArtistNotFoundException
import de.norm.events.event.EventNotFoundException
import de.norm.events.promoter.PromoterNotFoundException
import de.norm.events.venue.VenueNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Global exception handler translating the BFF's read-only domain exceptions into
 * RFC 9457 Problem Details. The public API only performs lookups, so the single
 * relevant failure mode is "resource not found" (404).
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(
        EventNotFoundException::class,
        VenueNotFoundException::class,
        ArtistNotFoundException::class,
        PromoterNotFoundException::class
    )
    fun handleNotFound(ex: RuntimeException): ProblemDetail {
        logger.debug { "Resource not found: ${ex.message}" }
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Resource not found")
    }
}
