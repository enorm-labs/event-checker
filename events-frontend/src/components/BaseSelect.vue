<script lang="ts" setup>
/**
 * A native `<select>` carrying the same field chrome as {@link BaseInput}, used by the filter
 * bars. Native for the same reasons — the platform's own dropdown on mobile, and an e2e suite
 * that drives these with Playwright's `selectOption` — so this is not the registry's Reka-based
 * `Select`, which renders a listbox instead of a real form control.
 *
 * The options go in the default slot; `aria-label` and the `@change` handler fall through.
 */
import type { HTMLAttributes } from 'vue'
import { cn, FIELD_CLASS } from '@/lib/utils'

const props = defineProps<{
  /** Current selection. One-way: these selects apply through `@change`, not `v-model`. */
  modelValue?: string
  class?: HTMLAttributes['class']
}>()
</script>

<template>
  <!--
    The accessible name arrives as a fall-through `aria-label` from the call site (all five
    currently pass one); the rule cannot see across the component boundary. This is a latent risk
    rather than a false alarm — nothing forces a future consumer to pass one — so the stronger fix
    is to make the label a required prop. Left as a follow-up because it changes the API of a
    shared component and every call site, which is more than this change should carry.
  -->
  <!-- eslint-disable-next-line vuejs-accessibility/form-control-has-label -->
  <select :class="cn(FIELD_CLASS, props.class)" :value="modelValue">
    <slot />
  </select>
</template>
