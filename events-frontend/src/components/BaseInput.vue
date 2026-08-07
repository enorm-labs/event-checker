<script lang="ts" setup>
/**
 * A native `<input>` carrying the shared field chrome, used by the filter bars.
 *
 * Deliberately a thin wrapper around the real element rather than a vendored shadcn primitive:
 * the filter bars depend on native behaviour (the browser's own date picker, `min`/`max` bounds,
 * `type="search"`) and the e2e suite drives real form controls.
 *
 * Supports `v-model` for the local drafts (search box, price range) and one-way `:model-value`
 * plus `@change` for the URL-driven controls. Everything else — `type`, `min`, `placeholder`,
 * `aria-label`, extra listeners — falls through to the input.
 */
import type { HTMLAttributes } from 'vue'
import { cn, FIELD_CLASS } from '@/lib/utils'

const props = defineProps<{
  modelValue?: string
  class?: HTMLAttributes['class']
}>()

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<template>
  <input
    :class="cn(FIELD_CLASS, props.class)"
    :value="modelValue"
    @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
  />
</template>
