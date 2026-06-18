<script lang="ts" setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Moon, Sun, CalendarDays } from '@lucide/vue'
import { Button } from '@/components/ui/button'

const message = ref<string>('')
const error = ref<string>('')
const isDark = ref<boolean>(false)

function toggleDark() {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
}

onMounted(async () => {
  try {
    const response = await fetch('/api/hello')
    if (!response.ok) {
      throw new Error(`HTTP error: ${response.status}`)
    }
    message.value = await response.text()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to fetch message'
  }
})
</script>

<template>
  <main class="mx-auto max-w-3xl space-y-8 p-8">
    <div class="flex items-center justify-between">
      <h1 class="text-3xl font-bold tracking-tight">Event Checker</h1>
      <Button
        variant="outline"
        size="icon"
        :aria-label="isDark ? 'Switch to light mode' : 'Switch to dark mode'"
        @click="toggleDark"
      >
        <Moon v-if="isDark" />
        <Sun v-else />
      </Button>
    </div>

    <p v-if="message" class="text-muted-foreground">{{ message }}</p>
    <p v-if="error" class="text-destructive">{{ error }}</p>

    <section class="space-y-3">
      <h2 class="text-sm font-medium text-muted-foreground">Variants</h2>
      <div class="flex flex-wrap gap-3">
        <Button>Default</Button>
        <Button variant="secondary">Secondary</Button>
        <Button variant="outline">Outline</Button>
        <Button variant="ghost">Ghost</Button>
        <Button variant="destructive">Destructive</Button>
        <Button variant="link">Link</Button>
      </div>
    </section>

    <section class="space-y-3">
      <h2 class="text-sm font-medium text-muted-foreground">Sizes &amp; icons</h2>
      <div class="flex flex-wrap items-center gap-3">
        <Button size="sm">Small</Button>
        <Button>Default</Button>
        <Button size="lg">Large</Button>
        <Button as-child>
          <RouterLink to="/calendar">
            <CalendarDays />
            Browse calendar
          </RouterLink>
        </Button>
      </div>
    </section>
  </main>
</template>
