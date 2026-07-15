<script lang="ts" setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import type { VenueSummary } from '@/api/types'
import { districtLabel } from '@/lib/districts'

const props = defineProps<{ venue: VenueSummary }>()

// A single "where" line: street address then district, skipping whatever is missing.
const location = computed(() =>
  [props.venue.address, districtLabel(props.venue.district)].filter(Boolean).join(' · '),
)
</script>

<template>
  <RouterLink
    :to="`/venues/${venue.slug}`"
    class="group flex gap-4 rounded-xl border border-border bg-card p-3 shadow-sm transition-all hover:border-primary/40 hover:shadow-md motion-safe:hover:-translate-y-0.5"
  >
    <img
      v-if="venue.imageUrl"
      :alt="venue.name ?? ''"
      :src="venue.imageUrl"
      class="size-20 shrink-0 rounded-lg object-cover grayscale-[0.5] transition duration-300 group-hover:grayscale-0"
      loading="lazy"
    />
    <div class="min-w-0 flex-1 space-y-1">
      <h3 class="truncate leading-tight font-semibold">{{ venue.name }}</h3>
      <p v-if="location" class="truncate text-sm text-muted-foreground">{{ location }}</p>
      <p v-else-if="venue.city" class="truncate text-sm text-muted-foreground">{{ venue.city }}</p>
    </div>
  </RouterLink>
</template>
