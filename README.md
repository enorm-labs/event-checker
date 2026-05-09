# Event Checker

A simple app/website for checking and finding music events in Berlin (scope might be extended in the future).

## TODO

See [TODO.md](./TODO.md)

## Vision / Roadmap / Ideas

See [VISION ROADMAP_IDEAS.md](./docs/VISION_ROADMAP_IDEAS.md)

## Architecture

* Frontend: Vue app (see [events-frontend](./events-frontend))
* Backend for Frontend: Spring Boot app with Kotlin, WebFlux and R2DBC (read-only?) (see [events-bff](./events-bff))
* Importer: Spring Boot app with Kotlin, WebFlux and R2DBC (see [events-importer](./events-importer))
* Database: PostgreSQL
* Search: Elasticsearch (maybe later)
* MGMT API and Frontend (maybe later)
* Android app (maybe later)
* AI agent / MCP server (maybe later)

## Development

For frontend development, see [events-frontend/README.md](./events-frontend/README.md).

### Setup JDK

Install SDKMAN to manage Java versions: https://sdkman.io/

```
# Use the Java version specified in .sdkmanrc
sdk env
```

### Build

```
./gradlew clean build
```

### Run

Start applications via IntelliJ or via Gradle like this:

```
./gradlew bootRun
```

This will also start the services (database) defined in the [compose.yaml](./compose.yaml) file via
Spring's [Docker Compose Support](https://docs.spring.io/spring-boot/reference/features/dev-services.html#features.dev-services.docker-compose).

### Swagger UI (OpenAPI)

When running an application locally, Swagger UI is available at:

* **events-bff**: http://localhost:8080/webjars/swagger-ui/index.html
* **events-importer**: http://localhost:8081/webjars/swagger-ui/index.html

The OpenAPI spec (JSON) is served at `/v3/api-docs` on the respective port.

### Check for dependency updates

```
./gradlew dependencyUpdates
```

Docs: https://github.com/ben-manes/gradle-versions-plugin

### Update the Gradlew Wrapper

```
# The following command upgrades the Wrapper to the latest version:
./gradlew wrapper --gradle-version latest

# The following command upgrades the Wrapper to a specific version:
./gradlew wrapper --gradle-version 9.5.0
```

Docs: https://docs.gradle.org/current/userguide/gradle_wrapper.html

### Lint and Formatting (ktlint)

```
## Lint
./gradlew ktlintCheck

## Format
./gradlew ktlintFormat
```
