package de.norm.events.promoter

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Admin REST controller for managing promoters.
 */
@RestController
@RequestMapping("/api/admin/promoters")
@Tag(name = "Admin: Promoters", description = "Admin CRUD endpoints for managing promoters")
class PromoterController(
    private val promoterService: PromoterService
) {
    @GetMapping
    fun findAll(
        @PageableDefault(size = 20, sort = ["name"]) pageable: Pageable
    ): Flow<PromoterResponse> = promoterService.findAll(pageable)

    @GetMapping("/{id}")
    suspend fun findById(
        @PathVariable id: Long
    ): PromoterResponse = promoterService.findById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(
        @Valid @RequestBody request: PromoterRequest
    ): PromoterResponse = promoterService.create(request)

    @PutMapping("/{id}")
    suspend fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: PromoterRequest
    ): PromoterResponse = promoterService.update(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun delete(
        @PathVariable id: Long
    ) = promoterService.delete(id)
}
