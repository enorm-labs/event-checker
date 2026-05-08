# Event Checker

A simple app/website for checking and finding music events in Berlin (scope might be extended in the future).

## TODO

1. Implement "Hello world" to verify that the setup works
    * Implement a simple REST endpoint in events-bff that returns "Hello world" when accessed via HTTP GET.
    * Setup vue app in events-frontend and make an HTTP GET request to the backend endpoint when the page loads. Display the response ("Hello world") on the
      page.
2. Collect event sources
3. Create initial data model and database schema. Use Flyway for database migrations.
4. Implement first event import job/controller to import events from one source and store them in the database.
5. Which scheduling solution to use? Spring's `@Scheduled` or something like JobRunr, Spring Cloud Dataflow, or Quartz?
6. Configure GitHub project
    * CI/CD workflows
    * Dependabot
    * Code Scanning
    * Copilot?
    * Settings
    * Project / Issues
    * Branch Protection Rules
7. Choose a Cloud Platform / Runtime environment
8. Deploy to Cloud Platform
9. Choose a License
10. Create Template Repository from this project in my Enterprise Repository and my private Repository on GitHub.
    * with `.github` directory and workflows, instructions, skills, prompts,and agents
    * with README, CONTRIBUTING, LICENSE, etc. (see GitHub docs and best practices)
    * check if there are any good existing templates already
    * see also OTR service template
    * add scaffolding?

## Vision / Idea

* Like Resident Advisor for Berlin events, but better
* Focus on all music events (concerts, club nights, festivals etc. - not only Techno/Electronic music) in venues (clubs, concert halls etc.) of Berlin (maybe
  later also other cities)
* Takeover more ideas from here: https://docs.google.com/document/d/1UkxdJECxvB6noW-n8dzX-r0du18M9-ggp6iWeapHpvI/edit?tab=t.0#heading=h.32gy0wklnx78

## Architecture

* Frontend: Vue app (see [events-frontend](./events-frontend))
* Backend for Frontend: Spring Boot app with Kotlin, WebFlux and R2DBC (see [events-bff](./events-bff))
* Importer: Spring Boot app with Kotlin, WebFlux and R2DBC (see [events-importer](./events-importer))
* Database: PostgreSQL
* Search: Elasticsearch (maybe later)
* MGMT API and Frontend (maybe later)
* Android app (maybe later)

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
