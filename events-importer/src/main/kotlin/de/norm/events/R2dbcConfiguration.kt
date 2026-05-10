package de.norm.events

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.data.relational.core.mapping.NamingStrategy

/**
 * Spring Data R2DBC configuration.
 *
 * - Enables auditing so `@CreatedDate` and `@LastModifiedDate` annotations on entity fields
 *   are automatically populated on save. This results in a dual-write for `updated_at`:
 *   Spring Data sets it in the entity before the UPDATE, and the PostgreSQL `set_updated_at()`
 *   trigger also sets it on every UPDATE. Both write `now()` so values are nearly identical.
 *   The DB triggers serve as a safety net for raw SQL paths (e.g. `@Query` updates) that
 *   bypass Spring Data's auditing infrastructure.
 * - Provides a custom [NamingStrategy] that applies the configured database schema globally,
 *   so individual `@Table` annotations don't need to repeat `schema = "events"`.
 *   The schema is read from `spring.r2dbc.properties.schema` in `application.yaml`.
 */
@Configuration
@EnableR2dbcAuditing
class R2dbcConfiguration {
    /**
     * Overrides the default [NamingStrategy] to qualify all generated SQL with the
     * configured schema (e.g. `events.venue` instead of just `"venue"`).
     *
     * Without this, Spring Data R2DBC generates unqualified table references for
     * derived query methods (`findBy*`, `save`, `delete`, etc.), which fail because
     * the tables live in a dedicated schema rather than `public`.
     */
    @Bean
    fun namingStrategy(
        @Value("\${spring.r2dbc.properties.schema}") schema: String
    ): NamingStrategy =
        object : NamingStrategy {
            override fun getSchema(): String = schema
        }
}
