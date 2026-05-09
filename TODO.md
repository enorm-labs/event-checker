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
    * Code Quality (Static Code Analysis) enabled https://github.com/enorm-labs/event-checker/settings/code-quality
    * Code/Security/License Scanning
        * Only available for free for public repositories: https://github.com/enorm-labs/event-checker/settings/security_analysis
        * GitHub action for Dependency Review: https://github.com/actions/dependency-review-action
6. Choose a Cloud Platform / Runtime environment
7. Deploy to Cloud Platform
8. Choose a License (Open Source)
9. Make repository public and enable Advanced Security Analysis
10. Create Template Repository from this project in my Enterprise Repository and my private Repository on GitHub.
    * with `.github` directory and workflows, instructions, skills, prompts,and agents
    * with README, CONTRIBUTING, LICENSE, etc. (see GitHub docs and best practices)
    * check if there are any good existing templates already
    * see also OTR service template
    * add scaffolding?
11. Create a Roadmap

### Technical TODOs

* Protect APIs (e.g. against DDoS)
    * Rate Limiting
    * Use Api Gateway?
* I18N and L10N
* Translations?
* What to consider before going live?
    * Imprint?
    * Display used FOSS?
    * DSGVO?
    * Accessibility?
    * Link to GitHub repository
    * Is it legal to scrape events from websites and display them on my website?
* Infrastructure and Tooling Update Checker (can dependabot also do this? Or use something like Renovate?)
