plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
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
