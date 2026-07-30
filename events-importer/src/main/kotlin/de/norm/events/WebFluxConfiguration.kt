package de.norm.events

import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer

/**
 * Registers the reactive [Pageable] argument resolver for WebFlux.
 *
 * This enables controllers to accept [org.springframework.data.domain.Pageable] parameters
 * that Spring resolves from `page`, `size`, and `sort` query parameters
 * (e.g. `?page=0&size=20&sort=name,asc`). Without this configuration, WebFlux cannot
 * construct a `Pageable` instance from request parameters.
 *
 * [StableSortPageableArgumentResolver] is used in place of Spring Data's
 * `ReactivePageableHandlerMethodArgumentResolver` so that every paged request carries a
 * unique final sort key and cannot repeat or skip rows across pages.
 */
@Configuration
class WebFluxConfiguration : WebFluxConfigurer {
    override fun configureArgumentResolvers(configurer: ArgumentResolverConfigurer) {
        configurer.addCustomResolver(StableSortPageableArgumentResolver())
    }
}
