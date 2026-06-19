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

    // Spring Modulith
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")

    // Spring Actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    testImplementation("org.springframework.boot:spring-boot-starter-data-r2dbc-test")
    implementation("org.springframework:spring-jdbc")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql")

    // Web
    implementation("org.springframework.boot:spring-boot-starter-webclient")
    testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")

    // SpringDoc OpenAPI – provides Swagger UI and OpenAPI spec generation for WebFlux
    // See: https://springdoc.org/#getting-started
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:${property("springdoc.version")}")

    // Logging — idiomatic SLF4J wrapper (see: https://github.com/oshai/kotlin-logging)
    implementation("io.github.oshai:kotlin-logging-jvm:${property("kotlin-logging.version")}")

    // Kotlin
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("tools.jackson.module:jackson-module-kotlin")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")

    // Dev Tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Testcontainers
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-r2dbc")

    // Flyway (test only) — the BFF owns no migrations; integration tests provision the schema
    // by running the importer's existing migrations (via a filesystem location, see Test config below).
    // This keeps the BFF's read entities verified against the real schema with zero DDL duplication.
    testImplementation("org.springframework.boot:spring-boot-starter-flyway")
    testImplementation("org.flywaydb:flyway-database-postgresql")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Point Flyway at the importer's migrations using an absolute filesystem path so the
    // location is independent of the test working directory.
    systemProperty(
        "spring.flyway.locations",
        "filesystem:${rootProject.projectDir}/events-importer/src/main/resources/db/migration"
    )
}
