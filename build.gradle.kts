import dev.detekt.gradle.Detekt
import org.jlleitschuh.gradle.ktlint.KtlintExtension

// Centralized dependency versions live in `gradle.properties` – change them there to update
// all subprojects at once.

// Plugins are applied in the subprojects, so that they are only applied to the relevant modules
plugins {
    kotlin("jvm") apply false
    kotlin("plugin.spring") apply false
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
    id("org.jetbrains.kotlinx.kover")
    id("org.jlleitschuh.gradle.ktlint") apply false
    id("dev.detekt") apply false
    id("io.github.ben-manes.versions")
    id("org.owasp.dependencycheck")
}

subprojects {
    group = "de.norm"
    version = "0.0.1-SNAPSHOT"

    apply(plugin = "org.jlleitschuh.gradle.ktlint") // Version should be inherited from parent
    apply(plugin = "dev.detekt")
    apply(plugin = "org.jetbrains.kotlinx.kover")

    repositories {
        mavenCentral()
    }

    // see https://github.com/jlleitschuh/ktlint-gradle?tab=readme-ov-file#configuration
    configure<KtlintExtension> {
        // The actual ktlint version, see https://github.com/pinterest/ktlint/releases
        version = "1.8.0"
    }

    // Detekt – static analysis for Kotlin. Customizations are defined in the root
    // detekt.yml config file. See https://detekt.dev/docs/introduction
    configure<dev.detekt.gradle.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("detekt.yml"))
    }
    tasks.withType<Detekt>().configureEach {
        jvmTarget = "25"
        reports {
            html.required.set(true)
            checkstyle.required.set(false)
            // SARIF reports are uploaded to GitHub Code Scanning for inline PR annotations
            sarif.required.set(true)
            // Markdown reports are used by CI to post detekt metrics to the job summary
            markdown.required.set(true)
        }
    }

    // Netty uses native libraries via System.loadLibrary() which requires explicit opt-in
    // on Java 22+. Without this flag, the JVM emits warnings and will block access in a
    // future release. See: https://openjdk.org/jeps/472
    tasks.withType<Test> {
        jvmArgs("--enable-native-access=ALL-UNNAMED")
        // The scraper suite parses several hundred KB of HTML fixtures into Jsoup DOMs while the
        // Testcontainers-backed integration tests hold their Spring contexts in the same worker JVM,
        // which outgrew Gradle's 512 MB default.
        maxHeapSize = "1g"
    }
    tasks.withType<JavaExec> {
        jvmArgs("--enable-native-access=ALL-UNNAMED")
    }
}

// Kover – aggregates test coverage from all subprojects into a single report.
// Run `./gradlew koverHtmlReport` to generate an HTML report at build/reports/kover/html/.
// Run `./gradlew koverLog` to print a coverage summary to the console.
dependencies {
    subprojects.forEach { kover(project(it.path)) }
}

// Per-module report filters do not propagate into this aggregated report, so mirror the
// events-core exclusions here to keep the headline aggregate meaningful: pure domain data
// classes, Spring Modulith markers, and test-fixture factories carry no logic and would only
// dilute the number. Exact class names are used for the domain data classes so the importer/BFF
// `*Entity` persistence classes stay measured; the `*Module` / `*Fixtures` patterns intentionally
// drop those non-logic classes across every module.
kover {
    reports {
        filters {
            excludes {
                classes(
                    "de.norm.events.artist.Artist",
                    "de.norm.events.event.Event",
                    "de.norm.events.event.LineupEntry",
                    "de.norm.events.genretag.GenreTag",
                    "de.norm.events.promoter.Promoter",
                    "de.norm.events.venue.Venue",
                    "de.norm.events.*Module",
                    "de.norm.events.*Fixtures"
                )
            }
        }
    }
}

// IntelliJ HTTP Client CLI – runs .http request files from the command line.
// Requires `ijhttp` to be installed (e.g. `brew install ijhttp` on macOS).
// Usage: `./gradlew httpTest` to run the full lifecycle scenario against a running importer.
tasks.register<Exec>("httpTest") {
    group = "verification"
    description = "Runs IntelliJ HTTP Client .http files against the local importer (requires ijhttp CLI and a running importer on port 8081)"
    workingDir = file("http")

    // Resolve the absolute path to ijhttp so that Gradle's Exec task can find it
    // even when /opt/homebrew/bin is not on the JVM's default PATH.
    val ijhttpPath =
        providers
            .exec {
                commandLine("bash", "-lc", "which ijhttp")
            }.standardOutput.asText
            .map { it.trim() }

    commandLine(
        ijhttpPath.get(),
        "--env-file",
        "http-client.env.json",
        "--env",
        "local",
        "-L",
        "VERBOSE",
        "full-lifecycle.http"
    )
}

// OWASP Dependency-Check – scans all project dependencies for known CVEs using the
// National Vulnerability Database (NVD). Run `./gradlew dependencyCheckAggregate` to
// produce a single report covering all subprojects.
// Reports land in `build/reports/dependency-check/` (the plugin's default since 13.0.0;
// it was `build/reports/` before). Both CI workflows upload from that path behind a
// `hashFiles` guard that skips silently, so keep them in step with any change here.
// See https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/
dependencyCheck {
    // `scanProjects` is deliberately NOT set: left empty, the plugin scans every project,
    // which is exactly what an aggregate report wants. Do not "restore" it as a list of
    // subproject *names* — the plugin matches `project.path` (`:events-core`), not
    // `project.name` (`events-core`), so a name list silently matches nothing and the scan
    // reports "Dependencies Scanned: 0" while the build stays green. See
    // AbstractAnalyze.shouldBeScanned: `scanProjects.isEmpty() || scanProjects.contains(project.path)`.
    // Build-tool classpaths, skipped because nothing on them is ever packaged or deployed: the
    // static-analysis and lint tools, and the Kotlin compiler plugin/script classpaths. They
    // pulled in their own (often much older) copies of Kotlin and logging libraries, which the
    // BOM overrides cannot reach and which produced findings against artifacts that only ever run
    // on a build agent — e.g. detekt's kotlin-reflect 1.6.10 and IntelliJ's repackaged coroutines.
    //
    // TRADE-OFF: this genuinely narrows the scan. A real CVE in detekt or ktlint will no longer
    // be reported here. That is accepted because those tools run only in CI and never process
    // untrusted input, and because Dependabot still watches them through the submitted dependency
    // graph. Do not extend this list to anything that ships.
    //
    // Names must match exactly — the plugin does `skipConfigurations.contains(configuration.name)`,
    // with no globbing — so a renamed or newly added tool configuration silently starts being
    // scanned again rather than erroring.
    skipConfigurations =
        listOf(
            "detekt",
            "detektPlugins",
            "ktlint",
            "ktlintBaselineReporter",
            "ktlintReporter",
            "ktlintRuleset",
            "kotlinCompilerPluginClasspathMain",
            "kotlinCompilerPluginClasspathTest",
            "kotlinScriptDef",
            "testKotlinScriptDef"
        )
    // Output formats: HTML for local review, SARIF for GitHub Code Scanning integration
    formats = listOf("HTML", "SARIF")
    // Fail the build if a CVE with CVSS score >= 7 (HIGH) is found
    failBuildOnCVSS = 7.0f
    // Suppress false positives via a shared suppression file (create as needed)
    suppressionFile = "owasp-suppressions.xml"
    // NVD API key speeds up database updates (rate-limited without it).
    // Set via NVD_API_KEY env var locally or as a GitHub Actions secret in CI.
    nvd.apiKey = System.getenv("NVD_API_KEY") ?: ""
    // Treat cached NVD data as valid for 24h before re-contacting the API. Combined
    // with caching the data directory in CI, this means most runs skip the NVD update
    // entirely instead of re-downloading on every build — the NVD API is frequently
    // rate-limited or returns 503s, and each contact is a chance to fail the scan.
    nvd.validForHours = 24
}
