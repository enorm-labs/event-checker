plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
    `java-test-fixtures`
}

group = "de.norm"
version = "0.0.1-SNAPSHOT"

// Use the same Java toolchain as events-bff and events-importer so the
// compiled library bytecode is compatible with its consumers.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}
