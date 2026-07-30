package de.norm.events

import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.reactive.BindingContext
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [StableSortPageableArgumentResolver].
 *
 * Asserts the tiebreaker is appended for both the `@PageableDefault` path and an explicit
 * client `sort` — the latter being the one the SPA actually uses, and the one a
 * default-only fix would have missed.
 *
 * The importer carries the same resolver and an equivalent test; this copy uses
 * `kotlin.test` assertions because Kotest is an importer-only test dependency.
 */
class StableSortPageableArgumentResolverTest {
    private val resolver = StableSortPageableArgumentResolver()

    /** Stand-in controller method supplying the `@PageableDefault` metadata the resolver reads. */
    @Suppress("UnusedParameter", "unused")
    private fun handler(
        @PageableDefault(size = 20, sort = ["name"]) pageable: Pageable
    ) = Unit

    private fun resolve(query: String): Pageable {
        val parameter = MethodParameter(javaClass.getDeclaredMethod("handler", Pageable::class.java), 0)
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/venues$query"))
        return resolver.resolveArgumentValue(parameter, BindingContext(), exchange)
    }

    private fun properties(query: String): List<String> = resolve(query).sort.toList().map { it.property }

    @Test
    fun `appends id to the declared default sort`() {
        assertEquals(Sort.by("name").and(Sort.by("id")), resolve("").sort)
    }

    @Test
    fun `appends id to an explicit client sort`() {
        // The SPA sends `sort`, which replaces @PageableDefault entirely — so this is the
        // path that actually has to be stabilised.
        assertEquals(Sort.by(Sort.Direction.ASC, "name").and(Sort.by("id")), resolve("?sort=name,asc").sort)
    }

    @Test
    fun `keeps the tiebreaker ascending regardless of the primary direction`() {
        val sort = resolve("?sort=eventDate,desc").sort
        assertTrue(sort.getOrderFor("eventDate")!!.isDescending)
        assertTrue(sort.getOrderFor("id")!!.isAscending)
    }

    @Test
    fun `appends id after every requested sort key`() {
        assertEquals(listOf("eventDate", "title", "id"), properties("?sort=eventDate,desc&sort=title,asc"))
    }

    @Test
    fun `does not append a second id order when the caller already sorts by id`() {
        assertEquals(listOf("id"), properties("?sort=id,desc"))
        assertEquals(listOf("name", "id"), properties("?sort=name,asc&sort=id,desc"))
    }

    @Test
    fun `preserves the requested page and size`() {
        val pageable = resolve("?page=3&size=50")
        assertEquals(3, pageable.pageNumber)
        assertEquals(50, pageable.pageSize)
    }
}
