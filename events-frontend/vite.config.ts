import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import tailwindcss from '@tailwindcss/vite'
import vueI18n from '@intlify/unplugin-vue-i18n/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    tailwindcss(),
    // Precompiles the message catalogues at build time so the vue-i18n *message compiler* is not
    // shipped to the browser. Without it the runtime is meaningfully larger and every message is
    // compiled on first use. See docs/adr/ADR-013_LOCALISATION.md.
    vueI18n({ include: fileURLToPath(new URL('./src/i18n/messages/**/*.json', import.meta.url)) }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      // Forward /api requests to the Spring Boot backend (events-bff)
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
