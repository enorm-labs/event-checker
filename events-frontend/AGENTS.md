# AGENTS.md

## Agent Instructions

- **No unsolicited git commits/pushes**: Never run `git commit`, `git push`, or `git rebase` (squash) unless explicitly asked to by the user.
- **Build verification**: Always run `npm run build` after finishing an implementation to verify that the project
  compiles (type-check + Vite build) without errors.
- **Lint & format**: Run `npm run lint` and `npm run format` before finishing to ensure code passes oxlint, eslint,
  and oxfmt checks.
- **GitHub CLI (`gh`)**: The `gh` CLI is installed (Homebrew) and authenticated. Use it for GitHub interactions.

## Project Overview

`events-frontend` is a **Vue 3 SPA** for discovering music events in Berlin. It is the user-facing frontend of the
Event Checker system. It communicates with the backend (`events-bff`) via REST API calls proxied through Vite's
dev server (`/api` → `http://localhost:8080`).

**Tech stack:**

- **Vue 3** (Composition API with `<script setup>`)
- **TypeScript 6**
- **Vite 8** (build tool & dev server)
- **Vue Router** (client-side routing)
- **Tailwind CSS v4** + **shadcn-vue** (styling & accessible component primitives — see ADR-010)
- **oxlint + oxfmt** (primary linter & formatter — fast, Rust-based)
- **eslint** (supplementary linting, integrated with oxlint via `eslint-plugin-oxlint`)
- **Vitest** (unit tests, jsdom environment)
- **Playwright** (end-to-end tests, multi-browser)

**Node version**: `^20.19.0 || >=22.12.0` (enforced via `engines` in `package.json`).

This project is **not** a Gradle subproject — it is managed separately via npm and has its own CI workflow
(`build-frontend.yml`).

## Build & Dev Commands

```bash
npm run dev        # Vite dev server (http://localhost:5173)
npm run build      # Type-check (vue-tsc) + production build
npm run preview    # Preview production build locally (http://localhost:4173)
npm run test:unit  # Vitest unit tests (jsdom, watch mode)
npm run test:unit:coverage  # Unit tests with V8 coverage report
npm run test:e2e   # Playwright end-to-end tests (chromium, firefox, webkit)
npm run lint       # oxlint (--fix) + eslint (--fix --cache)
npm run format     # oxfmt formatter
```

## Project Structure

```
events-frontend/
├── src/
│   ├── main.ts              # App entry point
│   ├── App.vue              # Root component
│   ├── router/index.ts      # Vue Router configuration
│   ├── composables/         # Reusable stateful logic (use* functions)
│   ├── views/               # Route-level page components
│   ├── components/          # Reusable UI components
│   │   ├── ui/              # shadcn-vue components (vendored; `npx shadcn-vue add`)
│   │   └── __tests__/       # Unit tests (colocated)
│   ├── lib/                 # Shared helpers (e.g. utils.ts → cn() classnames helper)
│   └── assets/              # Static assets (main.css holds the theme tokens, images)
├── e2e/                     # Playwright end-to-end tests
├── public/                  # Static files served as-is
├── index.html               # HTML entry point
├── vite.config.ts           # Vite configuration
├── vitest.config.ts         # Vitest configuration
├── playwright.config.ts     # Playwright configuration
├── eslint.config.ts         # ESLint flat config
├── .oxlintrc.json           # oxlint configuration
├── tsconfig.json            # TypeScript project references
├── tsconfig.app.json        # App source TS config
├── tsconfig.node.json       # Node/config files TS config
└── tsconfig.vitest.json     # Test files TS config
```

## Code Conventions

### General

- **Reference docs**: [Vue 3 Guide](https://vuejs.org/guide/introduction.html) |
  [Vue Style Guide](https://vuejs.org/style-guide/) |
  [Vue Router](https://router.vuejs.org/) |
  [Vite](https://vite.dev/)
- **Composition API only** — always use `<script setup lang="ts">`. Do not use Options API.
- **TypeScript strict mode** — all code must be fully typed. Avoid `any`; prefer explicit interfaces/types.
- **Path alias** — use `@/` to reference `src/` (configured in `vite.config.ts` and `tsconfig.app.json`).
- **No semicolons** — the project uses oxfmt which omits semicolons (consistent with current codebase style).
- **Single quotes** for string literals (enforced by formatter).

### Component Conventions

- **SFC structure order**: `<script setup>` → `<template>` → `<style scoped>`.
- **Component naming**: PascalCase for filenames (`HelloWorld.vue`, `TheWelcome.vue`).
  - Prefix `The` for singleton layout components (e.g. `TheNavbar.vue`, `TheFooter.vue`).
  - Prefix `Base` for presentational/dumb components that wrap HTML elements (e.g. `BaseButton.vue`, `BaseInput.vue`).
  - Views (page-level route components) go in `src/views/` with `*View.vue` suffix.
  - Reusable components go in `src/components/`.
- **Multi-word names** — component names must always be multi-word (`TodoItem`, not `Item`) to avoid conflicts with
  HTML elements. Only the root `App.vue` is exempt.
- **Full words over abbreviations** — prefer `UserProfileOptions.vue` over `UProfOpts.vue`.
- **Props**: Define with `defineProps<T>()` using TypeScript interface syntax. Use `withDefaults()` for default values.
- **Emits**: Define with `defineEmits<T>()` using TypeScript interface syntax.
- **Scoped styles**: Always use `<style scoped>` to prevent style leakage.

### Styling & UI Components (Tailwind v4 + shadcn-vue)

See [ADR-010](../docs/adr/ADR-010_FRONTEND_STYLING_FRAMEWORK.md) for the decision and rationale.

- **Styling** — use **Tailwind utility classes** in templates. Avoid hand-written CSS; reach for `<style scoped>`
  only when utilities genuinely can't express something. Global styles and the design tokens live in
  `src/assets/main.css`.
- **Theming** — the colour/radius/typography tokens are **CSS variables** in `src/assets/main.css`
  (`:root` for light, `.dark` for dark mode). Re-theme by editing those variables — do **not** hardcode hex
  colours in components; use the semantic Tailwind tokens (`bg-background`, `text-foreground`, `bg-primary`,
  `text-muted-foreground`, `border-border`, etc.).
- **Components** — add shadcn-vue components with `npx shadcn-vue@latest add <name>` (e.g. `button`, `card`,
  `dialog`). They are generated into `src/components/ui/<name>/` and are **owned by us** — edit them freely;
  they are not managed/upgraded by npm. Import via the `@/components/ui/...` alias.
- **Updating a `ui/` component to a newer registry version** — there is no automatic upgrade (we own the
  code). `--overwrite` **replaces the file wholesale; it does not merge**, so use git as the reconciliation
  tool:
  1. `npx shadcn-vue@latest diff <name>` — check whether the registry version differs from ours.
  2. Ensure the component has **no uncommitted changes**, then
     `npx shadcn-vue@latest add <name> --overwrite` to pull the latest.
  3. Review `git diff` to see both the upstream change and anything it clobbered, then reconcile
     (keep upstream, re-apply our customizations, or `git checkout` to revert).

  For a component we have customized, prefer hand-porting the change shown by `diff` instead of overwriting.
  This applies **only** to vendored `src/components/ui/**` components — our own components (e.g.
  `EventCalendar.vue`) are not registry-managed.

- **Class merging** — compose conditional classes with the `cn()` helper from `@/lib/utils`
  (clsx + tailwind-merge), as the generated components do.
- **Icons** — use **`@lucide/vue`** (`import { CalendarDays } from '@lucide/vue'`) for new icons.
- **Accessibility** — shadcn-vue components are built on Reka UI primitives and are accessible by default
  (focus management, ARIA, keyboard nav). Preserve that — don't strip ARIA attributes or `:as`/slot wiring
  when customizing.
- **Naming exemption** — `vue/multi-word-component-names` is disabled for `src/components/ui/**` in
  `eslint.config.ts` because shadcn components use single-word names (`Button`, `Card`) by design. This
  exemption applies **only** to vendored `ui/` components; your own components still follow the multi-word
  rule below.

### Template Conventions (per [Vue Style Guide](https://vuejs.org/style-guide/))

- **Always use `:key` with `v-for`** — required for correct DOM patching and animations.
- **Never combine `v-if` and `v-for`** on the same element — use a computed property to filter, or wrap with `<template v-for>`.
- **Self-closing components** — use `<MyComponent/>` not `<MyComponent></MyComponent>` (in SFCs).
- **PascalCase in templates** — use `<MyComponent/>` not `<my-component/>` in SFC templates.
- **Multi-attribute elements** — when an element has 2+ attributes, put each on its own line.
- **Simple template expressions** — move complex logic into `computed()` properties; templates should describe _what_,
  not _how_.
- **Directive shorthands consistently** — always use `:` (not `v-bind:`), `@` (not `v-on:`), `#` (not `v-slot:`).
- **Prop casing** — camelCase in declarations (`greetingText`), kebab-case when passed in templates (`greeting-text`).

### Composables

- Extract reusable stateful logic into **composables** — functions prefixed with `use` (e.g. `useCounter`, `useFetch`).
- Place composables in `src/composables/` with one composable per file.
- Return `readonly(ref)` from composables when consumers should not mutate internal state directly.
- Composables can use `ref`, `computed`, lifecycle hooks, and other composables — they are the primary code reuse
  mechanism in Vue 3 (replacing mixins).

### Reactivity

- Use **`ref`** for primitive values (`string`, `number`, `boolean`) and single references.
- Use **`reactive`** for objects/records where you want to avoid `.value` access.
- Do **not** destructure `reactive` objects without `toRefs()` — it breaks reactivity.
- Use `shallowRef` for large arrays/objects that are replaced wholesale (not mutated in place) — avoids deep
  reactivity overhead.
- Use `markRaw` for non-reactive third-party instances (e.g. chart libraries, maps).

### Routing

- Routes are defined in `src/router/index.ts`.
- Use **lazy loading** (dynamic `import()`) for non-critical routes to enable code splitting.
- Route names should be lowercase kebab-case strings.

### API Communication

- The Vite dev server proxies `/api` requests to the BFF backend at `http://localhost:8080`.
- Use `fetch` or a thin wrapper for HTTP calls — no heavy HTTP client libraries needed.
- Type API responses with TypeScript interfaces matching the backend's response DTOs.

## Linting & Formatting

The project uses a two-tier linting strategy:

1. **oxlint** (primary) — fast Rust-based linter with plugins: `eslint`, `typescript`, `unicorn`, `oxc`, `vue`, `vitest`.
   Configured via `.oxlintrc.json`. Runs first with `--fix`.
2. **eslint** (secondary) — catches rules not covered by oxlint. Uses `eslint-plugin-oxlint` to disable rules
   already handled by oxlint (avoids duplicate warnings). Runs second with `--fix --cache`.
3. **oxfmt** — Rust-based formatter for consistent code style. Runs via `npm run format`.

**Important**: Do NOT add Prettier — the project uses oxfmt instead.

## Testing

### Unit Tests (Vitest)

- Test files are colocated with components: `src/components/__tests__/*.spec.ts`.
- Uses **jsdom** as the DOM environment.
- Use `@vue/test-utils` for component mounting and interaction.
- Use **`data-testid` attributes** for test selectors — decoupled from CSS classes and DOM structure.
- Test composables in isolation (no component mount needed — just call the function and assert on returned refs).
- Run with: `npm run test:unit` (watch mode) or `npm run test:unit -- --run` (single run).
- Run with coverage: `npm run test:unit:coverage` — prints summary to console and generates HTML report in `coverage/`.

### End-to-End Tests (Playwright)

- Test files live in `e2e/` directory with `*.spec.ts` extension.
- Tests run against **five projects**: Desktop Chromium, Firefox, WebKit, plus **Mobile Chrome (Pixel 5)
  and Mobile Safari (iPhone 12)** — the last two use ~390px viewports.
- Dev mode: runs against `http://localhost:5173` (Vite dev server, reuses existing).
- CI mode: builds first, then runs against `http://localhost:4173` (Vite preview server).
- Run with: `npm run test:e2e`. CI runs the **full matrix**; the `/verify` skill runs **chromium only** to stay fast.
- **Layout/responsive gotcha:** because `/verify` is chromium-only (desktop viewport), it will not catch
  regressions that only appear on the mobile projects — e.g. a wider header nav overflowing a ~390px screen
  and pushing a control off-screen (a real failure we hit). When touching the **app shell, header/nav, or any
  layout**, run the mobile projects locally before pushing:
  `npm run test:e2e -- --project="Mobile Chrome" --project="Mobile Safari"`. On CI such a break also _slows_
  the run — a failing interaction burns the 30s action timeout × 2 retries × 5 projects.

## CI/CD

The frontend has its own GitHub Actions workflow (`.github/workflows/build-frontend.yml`) that triggers
only when `events-frontend/**` files change. It performs:

1. Install dependencies (`npm ci`)
2. Lint (`npm run lint`)
3. Build (`npm run build`)
4. Unit test (`npm run test:unit -- --run`)
5. Playwright e2e test (`npm run test:e2e`)

Uses Node 24.

## Key Files

| Purpose                | Path                        |
| ---------------------- | --------------------------- |
| Package config         | `package.json`              |
| Vite config            | `vite.config.ts`            |
| Vitest config          | `vitest.config.ts`          |
| Playwright config      | `playwright.config.ts`      |
| ESLint config          | `eslint.config.ts`          |
| oxlint config          | `.oxlintrc.json`            |
| shadcn-vue config      | `components.json`           |
| Theme & global CSS     | `src/assets/main.css`       |
| shadcn UI components   | `src/components/ui/`        |
| TypeScript root config | `tsconfig.json`             |
| App entry point        | `src/main.ts`               |
| Root component         | `src/App.vue`               |
| Router                 | `src/router/index.ts`       |
| Stores                 | `src/stores/`               |
| Views (pages)          | `src/views/`                |
| Components             | `src/components/`           |
| Unit tests             | `src/components/__tests__/` |
| E2E tests              | `e2e/`                      |
