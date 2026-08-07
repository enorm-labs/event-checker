<script lang="ts" setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { Moon, Sun } from '@lucide/vue'
import { Button } from '@/components/ui/button'
import AppFooter from '@/components/AppFooter.vue'
import BaseBadge from '@/components/BaseBadge.vue'
import BrandLogo from '@/components/BrandLogo.vue'
import GitHubMark from '@/components/GitHubMark.vue'
import { pageTitle } from '@/composables/usePageTitle'
import { REPOSITORY_URL } from '@/lib/links'

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

// Same rule for the beta badge: one string behind both the tooltip and the accessible name.
// "beta" alone would be a useless link name for a screen-reader user reading links out of context.
const BETA_LABEL =
  'Event Junkie is in beta — data may be incomplete or out of date. See what that means.'
</script>

<template>
  <div class="flex min-h-screen flex-col bg-background text-foreground">
    <!-- Announces route changes to screen readers; visually hidden. -->
    <div aria-atomic="true" aria-live="polite" class="sr-only" role="status">
      {{ announcement }}
    </div>

    <!-- WCAG 2.4.1 Bypass Blocks: the header repeats on every route, so a keyboard or switch user
         would otherwise tab through the brand, four nav links and two icon controls before
         reaching content — on every navigation. Hidden until focused, and the first focusable
         element in the document. It targets the wrapper below rather than each view's own <main>
         so there is one target instead of seven to keep in sync. -->
    <a
      class="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 focus:z-50 focus:rounded-md focus:bg-background focus:px-4 focus:py-2 focus:text-foreground focus:ring-3 focus:ring-ring/50"
      href="#main-content"
    >
      Skip to content
    </a>

    <header class="border-b border-border">
      <!-- Below `sm` the row wraps: brand, beta badge and controls stay on the first line and the
           links drop to a second one. All of them in a single row overflow a ~390px viewport — see
           the header-overflow guard in e2e/smoke.spec.ts, which is what keeps this honest as items
           are added. -->
      <!-- Named because the footer contributes a second navigation landmark: with more than one,
           each needs a distinguishable accessible name so screen-reader users can tell the
           landmark list apart. e2e selectors address it by this name. -->
      <nav
        aria-label="Main"
        class="mx-auto flex max-w-5xl flex-wrap items-center gap-x-4 gap-y-3 p-4 text-sm font-medium sm:flex-nowrap sm:gap-6"
      >
        <RouterLink class="rounded-sm transition-opacity hover:opacity-80" to="/">
          <BrandLogo />
        </RouterLink>

        <!-- A link rather than a tooltip-only marker: a `title` is invisible on touch devices and
             is not reliably announced, so the explanation has to be reachable by clicking. Targets
             the About page's #beta section; the router's scrollBehavior handles the anchor. -->
        <RouterLink
          :aria-label="BETA_LABEL"
          :title="BETA_LABEL"
          class="mr-2 rounded-full transition-opacity hover:opacity-80"
          to="/about#beta"
        >
          <BaseBadge variant="outline">beta</BaseBadge>
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

    <!-- `tabindex="-1"` makes this focusable by the skip link without adding it to the tab order.
         `flex-1` pushes the footer to the bottom on short pages. -->
    <div id="main-content" class="flex-1" tabindex="-1">
      <RouterView />
    </div>

    <AppFooter />
  </div>
</template>
