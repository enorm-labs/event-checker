package de.norm.events

import de.norm.events.artist.ArtistNotFoundException
import de.norm.events.event.EventNotFoundException
import de.norm.events.promoter.PromoterNotFoundException
import de.norm.events.venue.VenueNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException

/**
 * Global exception handler that translates domain exceptions into
 * structured HTTP error responses using RFC 9457 Problem Details.
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(
        VenueNotFoundException::class,
        ArtistNotFoundException::class,
        PromoterNotFoundException::class,
        EventNotFoundException::class
    )
    fun handleNotFound(ex: RuntimeException): ProblemDetail {
        logger.debug { "Entity not found: ${ex.message}" }
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Entity not found")
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(ex: DataIntegrityViolationException): ProblemDetail {
        logger.warn { "Data integrity violation: ${ex.message}" }
        val detail = extractConstraintDetail(ex)
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail)
    }

    /**
     * Inspects the exception cause chain for a Postgres-specific constraint name
     * and returns a user-friendly message identifying which unique constraint was violated.
     * Falls back to a generic message if the constraint name cannot be determined.
     */
    private fun extractConstraintDetail(ex: DataIntegrityViolationException): String {
        val message = ex.mostSpecificCause.message ?: return DEFAULT_CONFLICT_MESSAGE
        // Postgres unique violation messages contain: Key (<column>)=(<value>) already exists.
        // The constraint name appears after "constraint" in the detail, e.g.:
        //   "ERROR: duplicate key value violates unique constraint "artist_slug_key""
        val constraintName = CONSTRAINT_NAME_PATTERN.find(message)?.groupValues?.get(1)
        return when {
            constraintName?.contains("source_id") == true -> "A record with the same source ID already exists."
            constraintName?.contains("slug") == true -> "A record with the same slug already exists."
            constraintName != null -> "Duplicate value violates unique constraint: $constraintName"
            else -> DEFAULT_CONFLICT_MESSAGE
        }
    }

    companion object {
        private const val DEFAULT_CONFLICT_MESSAGE = "A record with the same unique identifier already exists."

        /** Matches the constraint name in Postgres error messages like: unique constraint "artist_slug_key" */
        private val CONSTRAINT_NAME_PATTERN = Regex("""unique constraint "(\w+)"""")
    }

    /**
     * Translates Bean Validation failures into a structured RFC 9457 Problem Detail
     * with an `errors` extension property containing per-field error details,
     * making it easy for API consumers to programmatically identify which fields failed.
     */
    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidation(ex: WebExchangeBindException): ProblemDetail {
        val fieldErrors = ex.bindingResult.fieldErrors.map { mapOf("field" to it.field, "message" to it.defaultMessage) }
        logger.debug { "Validation failed: ${fieldErrors.joinToString("; ") { "${it["field"]}: ${it["message"]}" }}" }
        val detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed")
        detail.setProperty("errors", fieldErrors)
        return detail
    }

    /**
     * Handles [IllegalArgumentException] thrown when the database contains an enum value
     * that doesn't match any Kotlin enum constant (e.g. an unknown [ArtistRole] or [EventType]
     * from a manual DB edit or future migration).
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ProblemDetail {
        logger.error { "Illegal argument encountered: ${ex.message}" }
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ex.message ?: "An unexpected data inconsistency was detected."
        )
    }
}
