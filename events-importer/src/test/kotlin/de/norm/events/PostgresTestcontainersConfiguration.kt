package de.norm.events

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Docs: https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
 */
@TestConfiguration(proxyBeanMethods = false)
class PostgresTestcontainersConfiguration {
    @Bean
    @ServiceConnection(name = "postgres")
    fun postgresContainer(): PostgreSQLContainer = PostgreSQLContainer("postgres:18.3-alpine").withReuse(true)
}