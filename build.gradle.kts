import org.jlleitschuh.gradle.ktlint.KtlintExtension

// Plugins are applied in the subprojects, so that they are only applied to the relevant modules
plugins {
    kotlin("jvm") apply false
    kotlin("plugin.spring") apply false
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
    id("org.jlleitschuh.gradle.ktlint") apply false
    id("com.github.ben-manes.versions")
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint") // Version should be inherited from parent

    repositories {
        mavenCentral()
    }

    // see https://github.com/jlleitschuh/ktlint-gradle?tab=readme-ov-file#configuration
    configure<KtlintExtension> {
    }
}
