package de.norm.events

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI metadata for the BFF's generated spec and Swagger UI.
 *
 * Sets the document title and API version shown at the top of Swagger UI; without this, SpringDoc
 * falls back to a generic "OpenAPI definition" title and version "1.0".
 */
@Configuration
class OpenApiConfiguration {
    @Bean
    fun eventsBffOpenApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Events BFF")
                    .version("v1")
                    .description("Public read API backing the events frontend.")
            )
}
