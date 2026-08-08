import { globalIgnores } from 'eslint/config'
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import pluginVue from 'eslint-plugin-vue'
import pluginVueA11y from 'eslint-plugin-vuejs-accessibility'
import pluginPlaywright from 'eslint-plugin-playwright'
import pluginVitest from '@vitest/eslint-plugin'
import pluginOxlint from 'eslint-plugin-oxlint'
import skipFormatting from 'eslint-config-prettier/flat'

// To allow more languages other than `ts` in `.vue` files, uncomment the following lines:
// import { configureVueProject } from '@vue/eslint-config-typescript'
// configureVueProject({ scriptLangs: ['ts', 'tsx'] })
// More info at https://github.com/vuejs/eslint-config-typescript/#advanced-setup

export default defineConfigWithVueTs(
  {
    name: 'app/files-to-lint',
    files: ['**/*.{vue,ts,mts,tsx}'],
  },

  globalIgnores(['**/dist/**', '**/dist-ssr/**', '**/coverage/**']),

  ...pluginVue.configs['flat/essential'],
  vueTsConfigs.recommended,

  // Accessibility lint — the static half of the WCAG 2.1 AA target
  // (docs/LEGAL.md §12). Catches missing form labels, bad `alt`, redundant
  // roles and click handlers on non-interactive elements at lint time; the axe sweep in
  // e2e/a11y.spec.ts covers what static analysis cannot see (contrast, focus order, live regions).
  // Do not disable a rule here to make a build pass — fix the markup, or raise it.
  ...pluginVueA11y.configs['flat/recommended'],

  {
    name: 'app/a11y-overrides',
    files: ['**/*.vue'],
    rules: {
      // `label-has-for` defaults to requiring nesting AND a for/id pair. Wrapping the control in
      // its <label> is implicit association — valid HTML, correctly announced by screen readers,
      // and what the checkbox filters in EventFilterBar.vue do. Requiring `some` rather than
      // `every` restores the actual WCAG bar (1.3.1 / 4.1.2): a label must be associated, not
      // associated twice. This relaxes an over-strict default; it does not permit unlabelled
      // controls, which the rule still catches.
      'vuejs-accessibility/label-has-for': [
        'error',
        { required: { some: ['nesting', 'id'] }, allowChildren: true },
      ],
    },
  },

  {
    ...pluginPlaywright.configs['flat/recommended'],
    files: ['e2e/**/*.{test,spec}.{js,ts,jsx,tsx}'],
  },

  {
    ...pluginVitest.configs.recommended,
    files: ['src/**/__tests__/*'],
  },

  {
    // shadcn-vue components are vendored (copied in, not authored by us) and use
    // single-word names by design (Button, Card, Dialog, ...).
    name: 'app/shadcn-ui-overrides',
    files: ['src/components/ui/**/*.vue'],
    rules: {
      'vue/multi-word-component-names': 'off',
    },
  },

  ...pluginOxlint.buildFromOxlintConfigFile('.oxlintrc.json'),

  skipFormatting,
)
