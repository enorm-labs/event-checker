## TODO

1. events-importer: Implement first event import job/controller to import events from one source and store them in the database.
    * first check if website as been changed. if yes, import events. if no, do nothing.
    * which website scraper to use? Can AI help us parsing websites?
2. Which scheduling solution to use? Spring's `@Scheduled` or something like JobRunr, Spring Cloud Dataflow, or Quartz?
    * Overview of all Import Jobs (Dashboard)
    * When a source has been updated and imported last time?
    * Was import successful?
    * How many events have been imported?
    * How many events have been updated?
    * How many events have been deleted?
    * Retry Import Job
    * Disable/Enable Import Job
3. Create test data set that can be used for tests (test fixtures) and for populating the local database
4. Choose a Cloud Platform / Runtime environment
5. Deploy to Cloud Platform
6. Choose a License (Open Source)
7. Make repository public and enable Advanced Security Analysis
8. Create Template Repository from this project in my Enterprise Repository and my private Repository on GitHub.
    * with `.github` directory and workflows, instructions, skills, prompts,and agents
    * with README, CONTRIBUTING, LICENSE, etc. (see GitHub docs and best practices)
    * check if there are any good existing templates already
    * see also OTR service template
    * add scaffolding?
9. Create a Roadmap

### Technical TODOs

* Logging: Always add context to logs (e.g. event id, artist id, etc.)
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

### Organizational TODOs

* What to consider before allowing contributions and posting issues?
* Repository best practices
* See GitHub docs and best practices
* Go-Live checklist (legal, security, etc.)
