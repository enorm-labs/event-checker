<script lang="ts" setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import type { VenueSummary } from '@/api/types'
import CachedImage from '@/components/CachedImage.vue'
import EventPoster from '@/components/EventPoster.vue'
import { districtLabel } from '@/lib/districts'
import { useLocalePath } from '@/composables/useLocalePath'
import { CARD_CLASS } from '@/lib/utils'

const props = withDefaults(
  defineProps<{
    venue: VenueSummary
    /** Heading level for the card's name — see the same prop on `EventCard.vue` for why. */
    as?: 'h2' | 'h3' | 'h4'
  }>(),
  { as: 'h3' },
)

// A single "where" line: street address then district, skipping whatever is missing.
const location = computed(() =>
  [props.venue.address, districtLabel(props.venue.district)].filter(Boolean).join(' · '),
)

const localePath = useLocalePath()
</script>

<template>
  <RouterLink :to="localePath(`/venues/${venue.slug}`)" :class="CARD_CLASS">
    <CachedImage
      v-if="venue.imageUrl"
      :src="venue.imageUrl"
      :sources="venue.imageSources"
      :alt="venue.name ?? ''"
      aspect="aspect-[3/2]"
      sizes="(min-width: 640px) 474px, calc(100vw - 2rem)"
      img-class="grayscale transition duration-300 group-hover:grayscale-0"
    />
    <EventPoster v-else :title="venue.name" />
    <div class="min-w-0 space-y-1">
      <component :is="as" class="truncate text-card-title font-semibold">
        {{ venue.name }}
      </component>
      <p v-if="location" class="truncate text-meta text-muted-foreground">{{ location }}</p>
      <p v-else-if="venue.city" class="truncate text-meta text-muted-foreground">{{ venue.city }}</p>
    </div>
  </RouterLink>
</template>
