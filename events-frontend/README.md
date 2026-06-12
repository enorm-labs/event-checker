# Event Checker Frontend

Displays events (today's event overview, week overview, month overview), provides an event calendar and allows to search
for events.

The frontend is built with Vue and uses the [events-bff](../events-bff) as backend.

## Development

### Setup Node.js

* Install nvm to manage Node.js versions: https://github.com/nvm-sh/nvm
* Use the Node.js version specified in [.nvmrc](.nvmrc)

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

### Type-Check, Compile and Minify for Production

```sh
npm run build
```

### Run Unit Tests with [Vitest](https://vitest.dev/)

```sh
npm run test:unit
```

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

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).
