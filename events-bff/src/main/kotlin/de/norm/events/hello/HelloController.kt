package de.norm.events.hello

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Simple controller to verify that the application setup works end-to-end.
 * Returns "Hello world" on GET /hello.
 */
@RestController
class HelloController {
    @GetMapping("/hello")
    fun hello(): String = GREETING

    companion object {
        private const val GREETING = "Hello world"
    }
}
