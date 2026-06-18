# Event Checker Frontend

Displays events (today's event overview, week overview, month overview), provides an event calendar and allows to search
for events.

The frontend is built with Vue and uses the [events-bff](../events-bff) as backend. Styling uses
[Tailwind CSS v4](https://tailwindcss.com) with [shadcn-vue](https://www.shadcn-vue.com) components — see
[ADR-010](../docs/adr/ADR-010_FRONTEND_STYLING_FRAMEWORK.md) for the rationale.

## Development

### Setup Node.js

- Install nvm to manage Node.js versions: https://github.com/nvm-sh/nvm
- Use the Node.js version specified in [.nvmrc](.nvmrc)

```
# use the Node.js version specified in .nvmrc
nvm use

# if not installed:
nvm install

# Verify the Node.js version:
node -v

# Verify npm version:
npm -v
```

### Recommended Browser Setup

- Chromium-based browsers (Chrome, Edge, Brave, etc.):
  - [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd)
  - [Turn on Custom Object Formatter in Chrome DevTools](http://bit.ly/object-formatters)
- Firefox:
  - [Vue.js devtools](https://addons.mozilla.org/en-US/firefox/addon/vue-js-devtools/)
  - [Turn on Custom Object Formatter in Firefox DevTools](https://fxdx.dev/firefox-devtools-custom-object-formatters/)

### Type Support for `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default, so we replace the `tsc` CLI with `vue-tsc` for
type checking. In editors, we need [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) to make the
TypeScript language service aware of `.vue` types.

### Install dependencies

```sh
npm install --save --save-exact
```

### Update dependencies

```sh
# check for outdated dependencies and update package.json if necessary (exact/pinned versions are preferred for better reproducibility and security)
npm outdated

# Update versions in package.json

# update dependencies to the latest version according to the version ranges specified in package.json
npm update --save --save-exact
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### Format code

```sh
npm run format
```

Formatting is handled by [oxfmt](https://oxc.rs/). If you use IntelliJ IDEA, install the
[oxc plugin](https://plugins.jetbrains.com/plugin/27061-oxc) so the IDE formats code
identically to `npm run format` — without it, IntelliJ's built-in formatter disagrees with oxfmt.

### Type-Check, Compile and Minify for Production

```sh
npm run build
```

### Run Unit Tests with [Vitest](https://vitest.dev/)

```sh
npm run test:unit
```

### Run Unit Tests with Coverage

```sh
npm run test:unit:coverage
```

This prints a coverage summary to the console and generates a detailed HTML report in `coverage/index.html`.
Uses V8's native code coverage via `@vitest/coverage-v8`.

### Run End-to-End Tests with [Playwright](https://playwright.dev)

```sh
# Install browsers for the first run
npx playwright install

# When testing on CI, must build the project first
npm run build

# Runs the end-to-end tests
npm run test:e2e
# Runs the tests only on Chromium
npm run test:e2e -- --project=chromium
# Runs the tests of a specific file
npm run test:e2e -- tests/example.spec.ts
# Runs the tests in debug mode
npm run test:e2e -- --debug
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```

### Add UI components (shadcn-vue)

```sh
# Add a component — it is generated into src/components/ui/<name>/ and owned by us (edit freely)
npx shadcn-vue@latest add button
npx shadcn-vue@latest add card dialog
```

The theme (colours, radius, typography, light/dark mode) is defined as CSS variables in
[`src/assets/main.css`](src/assets/main.css). Re-theme by editing those variables — see
[ADR-010](../docs/adr/ADR-010_FRONTEND_STYLING_FRAMEWORK.md).

#### Updating a component to a newer registry version

Components are copied into the repo and owned by us, so there is no automatic upgrade. To pull a newer
upstream version, use git as a safety net — `--overwrite` replaces the file wholesale and does **not** merge:

```sh
# 1. Check whether the registry version differs from ours
npx shadcn-vue@latest diff button

# 2. On a clean working tree, overwrite with the latest version
npx shadcn-vue@latest add button --overwrite

# 3. Review what changed (and what it clobbered), then reconcile
git diff src/components/ui/button
```

If you have customized the component, prefer hand-porting the change shown by `diff` rather than
overwriting. This applies only to `src/components/ui/**` — your own components are not registry-managed.

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).
