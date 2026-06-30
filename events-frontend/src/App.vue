<script lang="ts" setup>
import { ref } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { Moon, Sun } from '@lucide/vue'
import { Button } from '@/components/ui/button'

// Dark-mode toggle lives in the app shell so the choice persists across page navigation.
const isDark = ref<boolean>(false)

function toggleDark() {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
}
</script>

<template>
  <div class="min-h-screen bg-background text-foreground">
    <header class="border-b border-border">
      <nav class="mx-auto flex max-w-5xl items-center gap-6 p-4 text-sm font-medium">
        <RouterLink
          class="text-muted-foreground hover:text-foreground [&.router-link-exact-active]:text-foreground"
          to="/"
        >
          Home
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
        <Button
          :aria-label="isDark ? 'Switch to light mode' : 'Switch to dark mode'"
          class="ml-auto"
          size="icon"
          variant="outline"
          @click="toggleDark"
        >
          <Moon v-if="isDark" />
          <Sun v-else />
        </Button>
      </nav>
    </header>

    <RouterView />
  </div>
</template>
