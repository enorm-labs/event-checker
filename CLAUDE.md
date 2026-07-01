# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

The full agent playbook — commands, architecture decisions, R2DBC/Modulith gotchas, and conventions — lives in @AGENTS.md. Read it before assuming defaults.

## Project skills

Slash commands available under `.claude/skills/`:

- `/code-review` — review the current diff
- `/commit-message` — generate a commit message from staged changes
- `/improve-test-coverage` — find and fill coverage gaps
- `/squash-commit-message` — write a squash commit message for the current branch
- `/update-dependencies` — bump backend and frontend dependencies safely
- `/verify` — run the full pre-PR sequence: backend `ktlintCheck detekt build koverLog` + frontend `type-check`, `lint`, `test:unit`, `test:e2e` (chromium)

## Multi-module note

This is a Gradle multi-project build (`events-core`, `events-bff`, `events-importer`) plus a standalone frontend (`events-frontend/`). Per-module `CLAUDE.md`
files can be added in any of those directories if module-specific guidance is needed — they're loaded automatically when working in that subtree.
