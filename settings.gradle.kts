rootProject.name = "event-checker"

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

// Plugin resolution must include Maven Central because the Spring Boot Gradle plugin
// is published there and may not always be mirrored on the Gradle Plugin Portal.
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm") version "2.4.0"
        kotlin("plugin.spring") version "2.4.10"
        id("org.springframework.boot") version "4.1.0"
        id("io.spring.dependency-management") version "1.1.7"
        id("org.jetbrains.kotlinx.kover") version "0.9.8"
        id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
        id("dev.detekt") version "2.0.0-alpha.5"
        id("com.github.ben-manes.versions") version "0.54.0"
        id("org.owasp.dependencycheck") version "12.2.2"
    }
}

// All three modules are included as subprojects so events-bff and events-importer
// can declare a project(":events-core") dependency without needing a published artifact.
include("events-core")
include("events-bff")
include("events-importer")
