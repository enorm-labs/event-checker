package de.norm.events

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(PostgresTestcontainersConfiguration::class)
class EventsBffApplicationTests {
    @Test
    fun contextLoads() {
    }
}