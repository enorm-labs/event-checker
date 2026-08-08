import { fileURLToPath } from 'node:url'
import { configDefaults, defineConfig, mergeConfig } from 'vitest/config'
// Explicit `.ts` — this is a Vite config file too, so the same `configLoader: 'native'` rule
// applies. See the note in vite.config.ts.
import viteConfig from './vite.config.ts'

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: 'jsdom',
      // Installs the i18n plugin for every mount — see src/test/setup.ts.
      setupFiles: ['src/test/setup.ts'],
      exclude: [...configDefaults.exclude, 'e2e/**'],
      root: fileURLToPath(new URL('./', import.meta.url)),
      coverage: {
        provider: 'v8',
        reporter: ['text', 'json-summary', 'html'],
        include: ['src/**/*.{ts,vue}'],
        exclude: ['src/**/__tests__/**', 'src/main.ts'],
      },
    },
  }),
)
