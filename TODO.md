## TODO

1. events-importer: Implement first event import job/controller to import events from one source and store them in the database.
    * check if more websites offer an RSS feed
2. Fix security issues https://github.com/enorm-labs/event-checker/security/dependabot
3. Add multiple agents.md files (or at least one for backend and one for frontend)
    * Create path-specific custom
      instructions https://docs.github.com/en/copilot/how-tos/copilot-on-github/customize-copilot/add-custom-instructions/add-repository-instructions#creating-path-specific-custom-instructions
4. Create more prompts, skills, or agents, e.g. for planning/creating a new feature (interview, documentation, implementation) or adr
    * How to add skills: https://docs.github.com/en/copilot/how-tos/copilot-on-github/customize-copilot/customize-cloud-agent/add-skills
    * Awesome Copilot: https://awesome-copilot.github.com/ --> Check what could be useful
    * Note: Personal/Global skills are currently a Copilot CLI feature. In IDE chat (JetBrains/VS Code), only project-level skills in .github/skills/ are
      supported. Global ~/.copilot/skills/ works with Copilot CLI.
    * UPDATE: IntelliJs GitHub Copilot Chat now supports provider "Copilot CLI" besides "Local"
    * See https://github.com/github/awesome-copilot/tree/main, https://github.com/obra/superpowers, and https://github.com/gsd-build/get-shit-done
    * Create a feature planning and spec creation agent/prompt, see https://github.com/github/spec-kit
    * Create an agent for reviews
    * Create a prompt/agent for updating documentation
    * Create a prompt/agent for security
    * Create a prompt/agent for UI/UX
    * Create a prompt/agent for refactoring code and improving code quality (making it better, more maintainable, and more readable, but not changing the
      functionality)
    * Create a prompt/agent for reviewing the overall architecture of the project
    * Maybe add BACKLOG.md, see https://www.codecentric.de/wissens-hub/blog/strukturierte-migration-mit-claude-code-context-engineering-statt-prompt-engineering
    * Want structured planning for big features? Add a prompt file (e.g., .github/prompts/feature-planning.prompt.md) that you invoke manually when needed.
      Include the brainstorm → spec → plan flow.
    * Want subagent-driven execution? You can ask any agent to "break this into tasks and use subagents for each one" — no plugin needed.
    * Want TDD enforcement? Add it as a rule in your AGENTS.md under Testing Patterns.
    * Recommendation: Don't install Superpowers. Your AGENTS.md is already well-structured and project-specific. Superpowers' always-on ceremony would slow you
      down for the small-to-medium tasks that make up most of your work. Instead, steal the good ideas (spec-first for complex features, task decomposition) and
      encode them as optional prompt files you invoke explicitly when the task warrants it.
    * Try out Repomix https://repomix.com/
        * GH Actions: https://repomix.com/guide/github-actions
5. Create test data set that can be used for tests (test fixtures) and for populating the local database
6. Add Authentication and Authorization (depending on Cloud Platform? What's the best practice or standard for Spring apps? What about Keycloak (at least
   locally for testing?)
7. Use Stitch for UI design? Alternatives?
8. Create dashboard for analysing the data (Apache Superset, Kibana, or Grafana? Or something else?)
9. Choose a Cloud Platform / Runtime environment
10. Deploy to Cloud Platform
11. Make repository public and enable Advanced Security Analysis
12. Create Template Repository from this project in my Enterprise Repository and my private Repository on GitHub.
    * with `.github` directory and workflows, instructions, skills, prompts,and agents
    * with README, CONTRIBUTING, LICENSE, etc. (see GitHub docs and best practices)
    * check if there are any good existing templates already
    * see also OTR service template
    * add scaffolding?
13. Create a Roadmap

### Technical TODOs

* Add checkov scan if it makes sense
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
* Check useful workflows: https://github.com/enorm-labs/event-checker/actions/new?category=security
* Housekeeping: When to delete events from the database?

### Organizational TODOs

* Generate Mermaid Diagram (domain class diagram) via gradle
* What to consider before allowing contributions and posting issues? (contributing.md, code_of_conduct.md, security.md, support.md)
    * see https://github.com/github-samples/gitfolio for example
* Repository best practices
* See GitHub docs and best practices
* Go-Live checklist (legal, security, SEO, monitoring, alerting, dashboards, backups, recovery, etc.)
