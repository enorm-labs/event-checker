package de.norm.events

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

/**
 * Verifies the modular structure of the events-bff application.
 *
 * Spring Modulith treats each direct sub-package of the application's root package as an application module and validates that module boundaries are
 * respected (e.g. no cyclic dependencies, only public API types are accessed from the outside).
 *
 * @see <a href="https://docs.spring.io/spring-modulith/reference/">Spring Modulith Reference</a>
 */
class ModularityTests {
    private val modules = ApplicationModules.of(EventsBffApplication::class.java)

    @Test
    fun `should have a valid modular structure`() {
        modules.verify()
    }

    @Test
    fun `should generate module documentation`() {
        // Writes Documenter output (component diagrams, module canvas) to
        // build/spring-modulith-docs so it can be reviewed or published.
        org.springframework.modulith.docs
            .Documenter(modules)
            .writeDocumentation()
    }
}
