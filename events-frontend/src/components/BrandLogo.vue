<script lang="ts" setup>
// Brand lockup: the pulse mark (soundwave + heartbeat) beside the wordmark.
// Matches the favicon (public/favicon.svg) and the directions in docs/BRANDING.md §4.
// The SVG is decorative — the adjacent text carries the accessible name.
</script>

<template>
  <span class="inline-flex items-center gap-2">
    <svg
      aria-hidden="true"
      class="h-6 w-auto shrink-0"
      viewBox="0 0 128 52"
      xmlns="http://www.w3.org/2000/svg"
    >
      <defs>
        <linearGradient id="ej-logo-pulse" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stop-color="#823feb" />
          <stop offset="1" stop-color="#d528ce" />
        </linearGradient>
      </defs>
      <path
        d="M6 26 H36 L42 26 L47 11 L54 41 L60 18 L65 30 L70 26 H84 L89 16 L94 36 L99 26 H122"
        fill="none"
        stroke="url(#ej-logo-pulse)"
        stroke-width="5"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </svg>
    <span class="text-lg font-bold tracking-tight text-foreground">
      Event <span class="text-primary">Junkie</span>
    </span>
  </span>
</template>
