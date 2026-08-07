<script lang="ts" setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { Moon, Sun } from '@lucide/vue'
import { Button } from '@/components/ui/button'
import BrandLogo from '@/components/BrandLogo.vue'
import GitHubMark from '@/components/GitHubMark.vue'
import { pageTitle } from '@/composables/usePageTitle'

const REPOSITORY_URL = 'https://github.com/enorm-labs/event-checker'

// Screen-reader route announcer. Client-side navigations don't move focus or re-read the
// page, so a changed document title goes unheard. Mirror the title into an aria-live region
// on each change. The initial load is skipped (via router.isReady) because assistive tech
// already announces the document then; announcing it again would be duplicate noise.
const announcement = ref('')
const router = useRouter()
let ready = false
router.isReady().then(() => {
  ready = true
})
watch(pageTitle, (title) => {
  if (ready) announcement.value = title
})

// Dark-mode toggle lives in the app shell so the choice persists across route navigation.
// The preference is stored in localStorage and applied before paint by an inline script in
// index.html; here we mirror that initial state so the toggle icon/label start out correct.
const THEME_KEY = 'theme'
const isDark = ref<boolean>(document.documentElement.classList.contains('dark'))

function toggleDark() {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  try {
    localStorage.setItem(THEME_KEY, isDark.value ? 'dark' : 'light')
  } catch {
    // Ignore storage failures (e.g. private mode); persistence is best-effort.
  }
}

// One source for the toggle's accessible name and its hover tooltip — they must not drift.
const themeToggleLabel = computed(() =>
  isDark.value ? 'Switch to light mode' : 'Switch to dark mode',
)
</script>

<template>
  <div class="min-h-screen bg-background text-foreground">
    <!-- Announces route changes to screen readers; visually hidden. -->
    <div aria-atomic="true" aria-live="polite" class="sr-only" role="status">
      {{ announcement }}
    </div>

    <header class="border-b border-border">
      <!-- Below `sm` the row wraps: brand + controls stay on the first line and the links drop to a
           second one. All seven items in a single row overflow a ~390px viewport — see the
           header-overflow guard in e2e/smoke.spec.ts. -->
      <nav
        class="mx-auto flex max-w-5xl flex-wrap items-center gap-x-4 gap-y-3 p-4 text-sm font-medium sm:flex-nowrap sm:gap-6"
      >
        <RouterLink class="mr-2 rounded-sm transition-opacity hover:opacity-80" to="/">
          <BrandLogo />
        </RouterLink>
        <div class="order-last flex w-full items-center gap-4 sm:order-none sm:w-auto sm:gap-6">
          <RouterLink
            class="text-muted-foreground hover:text-foreground [&.router-link-exact-active]:text-foreground"
            to="/events"
          >
            Events
          </RouterLink>
          <RouterLink
            class="text-muted-foreground hover:text-foreground [&.router-link-exact-active]:text-foreground"
            to="/venues"
          >
            Venues
          </RouterLink>
          <RouterLink
            class="text-muted-foreground hover:text-foreground [&.router-link-exact-active]:text-foreground"
            to="/calendar"
          >
            Calendar
          </RouterLink>
          <RouterLink
            class="text-muted-foreground hover:text-foreground [&.router-link-exact-active]:text-foreground"
            to="/about"
          >
            About
          </RouterLink>
        </div>
        <div class="ml-auto flex items-center gap-2">
          <!-- `title` is the hover tooltip only; `aria-label` still supplies the accessible name
               (it wins over `title`), so the two stay in sync deliberately. -->
          <Button
            :href="REPOSITORY_URL"
            aria-label="Source code on GitHub"
            as="a"
            rel="noopener"
            size="icon"
            target="_blank"
            title="Source code on GitHub"
            variant="outline"
          >
            <GitHubMark />
          </Button>
          <Button
            :aria-label="themeToggleLabel"
            :title="themeToggleLabel"
            size="icon"
            variant="outline"
            @click="toggleDark"
          >
            <Moon v-if="isDark" />
            <Sun v-else />
          </Button>
        </div>
      </nav>
    </header>

    <RouterView />
  </div>
</template>
