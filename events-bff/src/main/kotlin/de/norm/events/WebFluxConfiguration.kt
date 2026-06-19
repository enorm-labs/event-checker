package de.norm.events

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer

/**
 * WebFlux configuration for the BFF.
 *
 * - Registers Spring Data's reactive [Pageable] argument resolver so controllers can accept
 *   [org.springframework.data.domain.Pageable] parameters resolved from `page`, `size`, and
 *   `sort` query parameters (e.g. `?page=0&size=20&sort=eventDate,asc`).
 * - Configures CORS from the `app.cors.allowed-origins` property. In local development the
 *   Vite proxy makes requests same-origin, so this is empty by default and only needed when
 *   the SPA is served from a different origin than the BFF.
 */
@Configuration
class WebFluxConfiguration(
    @Value("\${app.cors.allowed-origins:}") private val allowedOrigins: List<String>
) : WebFluxConfigurer {
    override fun configureArgumentResolvers(configurer: ArgumentResolverConfigurer) {
        configurer.addCustomResolver(ReactivePageableHandlerMethodArgumentResolver())
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        if (allowedOrigins.isNotEmpty()) {
            registry
                .addMapping("/**")
                .allowedOrigins(*allowedOrigins.toTypedArray())
                .allowedMethods("GET")
        }
    }
}
