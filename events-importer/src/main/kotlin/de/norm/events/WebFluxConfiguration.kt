package de.norm.events

import org.springframework.context.annotation.Configuration
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer

/**
 * Registers Spring Data's reactive [Pageable] argument resolver for WebFlux.
 *
 * This enables controllers to accept [org.springframework.data.domain.Pageable] parameters
 * that Spring resolves from `page`, `size`, and `sort` query parameters
 * (e.g. `?page=0&size=20&sort=name,asc`). Without this configuration, WebFlux cannot
 * construct a `Pageable` instance from request parameters.
 */
@Configuration
class WebFluxConfiguration : WebFluxConfigurer {
    override fun configureArgumentResolvers(configurer: ArgumentResolverConfigurer) {
        configurer.addCustomResolver(ReactivePageableHandlerMethodArgumentResolver())
    }
}
