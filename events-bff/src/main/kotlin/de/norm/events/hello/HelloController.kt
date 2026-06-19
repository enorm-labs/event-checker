package de.norm.events.hello

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Simple controller to verify that the application setup works end-to-end.
 * Returns "Hello world" on GET /hello.
 */
@RestController
class HelloController {
    // Hidden from the OpenAPI docs: this is an internal setup smoke-test, not part of the public read API.
    @Hidden
    @GetMapping("/hello")
    fun hello(): String = GREETING

    companion object {
        private const val GREETING = "Hello world"
    }
}
