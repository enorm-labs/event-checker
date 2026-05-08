# Event Checker

A simple app/website for checking and finding music events in Berlin (scope might be extended in the future).

## TODO

1. Create initial data model and database schema by checking data from sources. Use Flyway for database migrations.
2. events-importer: Add endpoints for CRUD all data (venues, events, etc.) manually
3. events-importer: Implement first event import job/controller to import events from one source and store them in the database.
   * first check if website as been changed. if yes, import events. if no, do nothing.
   * which website scraper to use? Can AI help us parsing websites?
4. Which scheduling solution to use? Spring's `@Scheduled` or something like JobRunr, Spring Cloud Dataflow, or Quartz?
    * Overview of all Import Jobs (Dashboard)
    * When a source has been updated and imported last time?
    * Was import successful?
    * How many events have been imported?
    * How many events have been updated?
    * How many events have been deleted?
    * Retry Import Job
    * Disable/Enable Import Job
5. Configure GitHub project
    * CI/CD workflows
    * Dependabot
    * Code Scanning
    * Copilot?
    * Settings
    * Project / Issues
    * Branch Protection Rules
6. Choose a Cloud Platform / Runtime environment
7. Deploy to Cloud Platform
8. Choose a License
9. Create Template Repository from this project in my Enterprise Repository and my private Repository on GitHub.
   * with `.github` directory and workflows, instructions, skills, prompts,and agents
   * with README, CONTRIBUTING, LICENSE, etc. (see GitHub docs and best practices)
   * check if there are any good existing templates already
   * see also OTR service template
   * add scaffolding?
10. Create a Roadmap

## Vision / Idea

* Like Resident Advisor, Bandsintown, eventbrite, and songkick for Berlin events, but better
* Focus on all (and small) music events (concerts, club nights, festivals etc. - not only Techno/Electronic music) in venues (clubs, concert halls etc.) of Berlin (maybe
  later also other cities)
* Takeover more ideas from here: https://docs.google.com/document/d/1UkxdJECxvB6noW-n8dzX-r0du18M9-ggp6iWeapHpvI/edit?tab=t.0#heading=h.32gy0wklnx78
* Later:
    * Login / Profile
    * Favorite Venues
    * Favorite Artists
    * Notifications
    * Venue profile (with links)
    * Artist profile (with links)
    * User can say that he is interested or going to an event (RSVP)
    * Rank events by popularity (amount of RSVPs)
    * Rank events by artist popularity
    * Integrate Spotify, Deezer, Soundcloud, Resident Advisor for getting notifications when favorite artists perform at a venue
    * Create Club Map (Events nearby)

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
