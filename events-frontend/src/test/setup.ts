import { config } from '@vue/test-utils'

import { i18n } from '@/i18n'

/**
 * Global test setup.
 *
 * Installs the i18n plugin for every `mount()`, because any component calling `useI18n()` — via
 * `useFormat()`, or directly once strings are extracted in Phase 2 — throws without it. Doing it
 * here rather than per-spec means a component test never has to know whether the component it
 * mounts happens to render a translated string, which is exactly the sort of incidental knowledge
 * that makes tests brittle.
 *
 * It also mirrors production, where the plugin is always installed on the app.
 */
config.global.plugins = [i18n]
