<script lang="ts" setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="min-h-screen bg-background text-foreground">
    <header class="border-b border-border">
      <nav class="mx-auto flex max-w-3xl items-center gap-6 p-4 text-sm font-medium">
        <RouterLink
          to="/"
          class="text-muted-foreground hover:text-foreground [&.router-link-exact-active]:text-foreground"
        >
          Home
        </RouterLink>
        <RouterLink
          to="/calendar"
          class="text-muted-foreground hover:text-foreground [&.router-link-exact-active]:text-foreground"
        >
          Calendar
        </RouterLink>
        <RouterLink
          to="/about"
          class="text-muted-foreground hover:text-foreground [&.router-link-exact-active]:text-foreground"
        >
          About
        </RouterLink>
      </nav>
    </header>

    <RouterView />
  </div>
</template>
