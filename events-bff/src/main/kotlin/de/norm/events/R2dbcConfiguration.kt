package de.norm.events

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.relational.core.mapping.NamingStrategy

/**
 * Spring Data R2DBC configuration for the BFF.
 *
 * Provides a custom [NamingStrategy] that applies the configured database schema globally,
 * so individual `@Table` annotations don't need to repeat `schema = "events"`. The schema
 * is read from `spring.r2dbc.properties.schema` in `application.yaml`.
 *
 * Unlike the importer, the BFF does **not** enable `@EnableR2dbcAuditing`: it is a read-only
 * service and never populates `@CreatedDate`/`@LastModifiedDate`.
 */
@Configuration
class R2dbcConfiguration {
    /**
     * Overrides the default [NamingStrategy] to qualify all generated SQL with the
     * configured schema (e.g. `events.venue` instead of just `"venue"`).
     *
     * Without this, Spring Data R2DBC generates unqualified table references for derived
     * query methods (`findBy*`, `findAll`, etc.), which fail because the tables live in a
     * dedicated `events` schema rather than `public`.
     */
    @Bean
    fun namingStrategy(
        @Value("\${spring.r2dbc.properties.schema}") schema: String
    ): NamingStrategy =
        object : NamingStrategy {
            override fun getSchema(): String = schema
        }
}
