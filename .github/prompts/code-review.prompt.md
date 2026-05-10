# Code Review

Review the code changes in this pull request (or diff) for correctness, maintainability, and adherence to project
conventions.

## Context Gathering

1. **Understand the change** — read the PR description, linked issues, and conversation history to understand the
   intent and motivation behind the changes. Use `gh pr view` to fetch the PR description and metadata.
2. **Inspect the diff** — use `git --no-pager diff main..HEAD` (or `gh pr diff`) to see all changed files.
3. **Check CI status** — use `gh pr checks` to verify whether CI passes before reviewing code.
4. **Read surrounding code** — don't review changes in isolation. Read the full files and related code to understand
   how the changes fit into the existing architecture.
5. **Check AGENTS.md** — refer to the project's `AGENTS.md` for architecture decisions, conventions, and patterns
   that the code must follow.

## Review Checklist

### Correctness

- Does the code do what it claims to do?
- Are there off-by-one errors, race conditions, or null pointer risks?
- Are edge cases handled (empty collections, null/missing values, concurrent access)?
- For reactive/coroutine code: are suspending functions used correctly? Are there accidental blocking calls?

### Kotlin Conventions

- `val` preferred over `var`; no unnecessary mutability.
- No `!!` operator unless absolutely justified (with a comment explaining why).
- Data classes used for DTOs and value objects.
- Functional constructs (`map`, `filter`, `let`, `also`) used where they improve readability.
- Sealed classes for restricted hierarchies.
- Extension functions used appropriately.

### Spring Boot Patterns

- Constructor injection only (no `@Autowired` on fields, no `lateinit var`).
- Dependencies declared as `private val`.
- Services are stateless; business logic lives in `@Service` classes, not controllers.
- Controllers are thin — they delegate to services and handle HTTP concerns only.
- Proper use of `@ConfigurationProperties` with data classes for configuration.
- Repositories use `CoroutineCrudRepository` (reactive stack — no blocking APIs).
- Custom `@Query` SQL includes the schema prefix (e.g., `events.table_name`).

### Error Handling

- Domain exceptions follow the `*NotFoundException` naming pattern.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) catches and translates exceptions to RFC 9457 Problem Details.
- Appropriate HTTP status codes are returned (404, 409, 400, etc.).
- No swallowed exceptions — errors are logged or propagated.

### API Design

- Endpoints follow the project's path conventions (e.g., `/api/admin/<resource>` for importer admin endpoints).
- Request DTOs use Bean Validation annotations (`@Valid`, `@NotNull`, etc.).
- Response DTOs use `@Schema` annotations for OpenAPI documentation.
- Domain classes in `events-core` remain free of web/Swagger annotations.
- Controllers are annotated with `@Tag(name = "Admin: <Entity>")` for Swagger grouping.

### Testing

- New features and bug fixes include tests.
- Integration tests use `WebTestClient` with Testcontainers (extend `BaseControllerTest` in the importer).
- Tests use backtick function names for readability.
- MockK is used for mocking (not Mockito).
- Kotest assertions (`shouldBe`, `shouldContain`) are preferred.
- Test fixture factories provide sensible defaults; tests only override relevant properties.

### Code Organization

- Organized by feature/domain, not by layer.
- Each feature module follows the consistent file layout:
  `*Controller.kt`, `*Service.kt`, `*Repository.kt`, `*Entity.kt`, `*Request.kt`, `*Module.kt`, `*NotFoundException.kt`.
- `@ApplicationModule(allowedDependencies = [...])` declarations are present and correct.
- Imports are organized and unused imports removed.

### Security

- No secrets, API keys, or credentials in code or config files.
- Parameterized queries only (no string concatenation in SQL).
- Input validated before processing.

### Documentation

- Public API changes are reflected in `@Schema` annotations and OpenAPI docs.
- Non-obvious design decisions have code comments explaining _why_, not _what_.
- AGENTS.md is updated if new conventions or architectural patterns are introduced.

### Build & Dependencies

- New dependencies are necessary and justified.
- Dependency versions are centralized (root `build.gradle.kts` or `settings.gradle.kts`).
- `./gradlew build` passes (compilation, tests, ktlint, detekt).

## Review Output Format

For each finding, provide:

1. **File and line** — the specific location.
2. **Severity** — one of: 🔴 **blocker** (must fix), 🟡 **suggestion** (should fix), 🟢 **nit** (optional improvement).
3. **Description** — what the issue is and why it matters.
4. **Suggested fix** — a concrete code change or approach, not just "fix this".

Group findings by file. Start with blockers, then suggestions, then nits.

If the code looks good, say so — don't invent issues for the sake of having feedback.

## Review Output File (Mandatory)

**IMPORTANT: Every review MUST produce a markdown file. This is not optional — it is a required deliverable.**
**You MUST create (or update) the review file using a file-writing tool before ending your turn.**
After completing the review, **automatically write the full review to a markdown file** in the `docs/reviews/` directory.
Do not ask the user whether to write the file — just do it. Never present review findings only in the chat without
also writing them to the file.

- **File name**: `{branch}-review.md`, where `{branch}` is the current Git branch name (use `git rev-parse --abbrev-ref HEAD`).
  Replace any `/` characters in the branch name with `-` to produce a valid filename (e.g., branch `feat/add-venues` → `feat-add-venues-review.md`).
- **Create the `docs/reviews/` directory** if it does not already exist.
- The file should contain the complete review output, including all findings, severities, and suggested fixes.
- **If the review file already exists**, read the existing file first and update it in place:
    - For each previously reported finding that has been resolved in the current code, mark it as done by
      prefixing the description with ~~strikethrough~~ and adding a `✅ Resolved` label.
    - Keep unresolved findings unchanged so they remain visible as open items.
    - Append any **new findings** discovered in this review pass to the appropriate section.
    - Add a `## Review History` section at the bottom (or update it if it already exists) with a timestamped entry
      noting which findings were resolved and which are new (e.g., `- 2026-05-12: 2 resolved, 1 new finding added`).
