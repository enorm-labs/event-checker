plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
    `java-library`
    `maven-publish`
    `java-test-fixtures`
}

// Use the same Java toolchain as events-bff and events-importer so the
// compiled library bytecode is compatible with its consumers.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(property("java.version").toString().toInt())
    }
    withSourcesJar()
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
    // Spring Modulith
    api("org.springframework.modulith:spring-modulith-starter-core")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")

    // Kotlin Logging — idiomatic SLF4J wrapper for Kotlin
    // See: https://github.com/oshai/kotlin-logging
    implementation("io.github.oshai:kotlin-logging-jvm:${property("kotlin-logging.version")}")

    testImplementation(kotlin("test"))
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
    jvmToolchain(property("java.version").toString().toInt())
}

tasks.test {
    useJUnitPlatform()
}

// events-core is a pure domain library: almost every class is a plain data class, a Spring
// Modulith marker, or a test-fixture factory with no executable logic, so Kover counts their
// synthetic/unused members as "uncovered" and drives the module coverage down to a meaningless
// number (~11%). Exclude those so the metric reflects the code that actually carries logic — the
// enum `parseOrDefault` companions (EventType/EventStatus/ArtistRole) and MoneyExtensions, both of
// which remain measured and tested.
kover {
    reports {
        filters {
            excludes {
                classes(
                    // Plain domain data classes — no logic, only synthetic members.
                    "de.norm.events.artist.Artist",
                    "de.norm.events.event.Event",
                    "de.norm.events.event.LineupEntry",
                    "de.norm.events.genretag.GenreTag",
                    "de.norm.events.promoter.Promoter",
                    "de.norm.events.venue.Venue",
                    // Spring Modulith `@ApplicationModule` markers — no logic (`*` spans package segments).
                    "de.norm.events.*Module",
                    // Published test-fixture factories (java-test-fixtures) — test support, not production code.
                    "de.norm.events.*Fixtures"
                )
            }
        }
    }
}
