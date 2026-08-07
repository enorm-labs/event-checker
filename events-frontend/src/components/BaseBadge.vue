<script lang="ts" setup>
/**
 * The small pill used for statuses, taxonomies and tags across the cards and the event detail
 * page. Variants rather than per-call-site classes so "sold out" reads the same everywhere and a
 * new pill can't invent its own colour.
 */
import { cva, type VariantProps } from 'class-variance-authority'
import type { HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'

const badgeVariants = cva('rounded-full px-2 py-0.5 text-xs font-medium', {
  variants: {
    variant: {
      /** Neutral tag — genres. */
      muted: 'bg-muted text-muted-foreground',
      /** Outlined, for a different taxonomy sharing the row with filled pills (type, stage). */
      outline: 'border border-border text-foreground/70',
      /** Free / available. */
      success: 'bg-success/10 text-success',
      /** Sold out, cancelled, and other non-scheduled statuses. */
      destructive: 'bg-destructive/10 text-destructive',
    },
  },
  defaultVariants: {
    variant: 'muted',
  },
})

type BadgeVariants = VariantProps<typeof badgeVariants>

const props = defineProps<{
  variant?: BadgeVariants['variant']
  class?: HTMLAttributes['class']
}>()
</script>

<template>
  <span :class="cn(badgeVariants({ variant }), props.class)">
    <slot />
  </span>
</template>
