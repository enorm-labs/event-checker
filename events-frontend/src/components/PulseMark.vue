<script lang="ts" setup>
import { useId } from 'vue'

// The brand pulse mark (soundwave + heartbeat), reused by the nav logo (BrandLogo) and the
// home hero. `animate` draws the stroke in on mount and adds a subtle beat; both are gated
// behind prefers-reduced-motion in main.css. Size it with a height utility (e.g. `h-6`).
withDefaults(defineProps<{ animate?: boolean }>(), { animate: false })

// Unique gradient id per instance so multiple marks on one page don't collide.
const gradientId = useId()
</script>

<template>
  <svg
    :class="['w-auto', { 'ej-beat': animate }]"
    aria-hidden="true"
    fill="none"
    viewBox="0 0 128 52"
    xmlns="http://www.w3.org/2000/svg"
  >
    <defs>
      <linearGradient :id="gradientId" x1="0" y1="0" x2="1" y2="0">
        <stop offset="0" stop-color="#823feb" />
        <stop offset="0.7" stop-color="#a24df2" />
        <stop offset="1" stop-color="#d528ce" />
      </linearGradient>
    </defs>
    <path
      :class="{ 'ej-draw': animate }"
      :stroke="`url(#${gradientId})`"
      d="M6 26 H36 L42 26 L47 11 L54 41 L60 18 L65 30 L70 26 H84 L89 16 L94 36 L99 26 H122"
      pathLength="1"
      stroke-linecap="round"
      stroke-linejoin="round"
      stroke-width="5"
    />
  </svg>
</template>
