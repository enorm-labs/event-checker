plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(property("java.version").toString().toInt())
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:${property("spring-modulith.version")}")
    }
}

dependencies {
    // Shared domain model and utilities from the events-core library module
    implementation(project(":events-core"))
    testImplementation(testFixtures(project(":events-core")))

    // Spring Modulith – enforces modular application structure and provides
    // event publication registry, observability, and documentation support.
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")

    // Spring Actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    testImplementation("org.springframework.boot:spring-boot-starter-data-r2dbc-test")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework:spring-jdbc")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql")

    // Validation – Bean Validation API for request body validation (@Valid, @NotBlank, etc.)
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Web
    implementation("org.springframework.boot:spring-boot-starter-webclient")
    testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")

    // SpringDoc OpenAPI – provides Swagger UI and OpenAPI spec generation for WebFlux
    // See: https://springdoc.org/#getting-started
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:${property("springdoc.version")}")

    // Kotlin
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("tools.jackson.module:jackson-module-kotlin")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")

    // Kotlin Logging – idiomatic Kotlin wrapper around SLF4J
    // See: https://github.com/oshai/kotlin-logging
    implementation("io.github.oshai:kotlin-logging-jvm:${property("kotlin-logging.version")}")

    // Slugify – generates URL-friendly slugs from arbitrary strings, with locale support
    // See: https://github.com/slugify/slugify
    implementation("com.github.slugify:slugify:${property("slugify.version")}")

    // Jsoup – robust HTML parser and CSS-selector-based scraper for importing
    // event data from venue websites. Used for parsing only (HTTP fetching goes
    // through Spring WebClient). See: https://jsoup.org/
    implementation("org.jsoup:jsoup:${property("jsoup.version")}")

    // Dev Tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Kotest assertions – expressive matchers for readable test assertions
    // See: https://kotest.io/docs/assertions/assertions.html
    testImplementation("io.kotest:kotest-assertions-core:${property("kotest.version")}")

    // MockK – idiomatic Kotlin mocking library, preferred over Mockito for Kotlin tests
    // See: https://mockk.io/
    testImplementation("io.mockk:mockk:${property("mockk.version")}")

    // Testcontainers
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-r2dbc")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
