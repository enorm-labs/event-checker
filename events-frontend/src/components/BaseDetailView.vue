<script lang="ts" setup>
import { RouterLink } from 'vue-router'
import type { EventPage } from '@/api/types'
import { Button } from '@/components/ui/button'
import type { ImageSource } from '@/api/types'

import CachedImage from '@/components/CachedImage.vue'
import ImageCreditLine from '@/components/ImageCreditLine.vue'
import EventCard from '@/components/EventCard.vue'
import SectionLabel from '@/components/SectionLabel.vue'
import type { ImageCredit } from '@/lib/imageCredit'
import { CARD_GRID_CLASS } from '@/lib/utils'
import { useLocalePath } from '@/composables/useLocalePath'
import { useI18n } from 'vue-i18n'

/**
 * Presentational shell shared by the artist, venue, and promoter detail pages. Owns the
 * loading / not-found / error scaffold, the hero header (image + kind label + name), and both
 * event feeds, so each view only wires its data and fills the entity-specific slots.
 *
 * Slots:
 * - `meta` — entity-specific header metadata rendered under the name (links, address, …).
 * - default — optional content between the header and the events feed (e.g. a description).
 */
defineProps<{
  /** Entity kind label, e.g. "Artist" — shown above the name and in the not-found heading. */
  kind: string
  /** Entity fetch state (from `useAsync`). */
  loading: boolean
  error: string | null
  notFound: boolean
  /** Whether the entity has loaded — guards the content branch. */
  ready: boolean
  /** Copy shown under the "<kind> not found" heading. */
  notFoundText: string
  /** Resolved entity display fields. */
  name?: string | null
  imageUrl?: string | null
  imageSources?: ImageSource[] | null
  intrinsicWidth?: number | null
  intrinsicHeight?: number | null
  /** Who to credit for the image, or null where it carries no credit (#1275). */
  credit?: ImageCredit | null
  /** Upcoming-events feed state (from `useEventSearch`). */
  events: EventPage | null
  eventsLoading: boolean
  eventsError: string | null
  /** Copy shown when the events feed is empty. */
  emptyText: string
  /**
   * The past-events feed. No loading or error state: the section appears only once it has events,
   * so a slow or failed archive is absent rather than noisy.
   */
  pastEvents?: EventPage | null
}>()

// The document title is *not* set here. Each detail view owns its own page meta, because the
// description and image come from its entity and this component only ever sees a name — see
// lib/pageMeta.ts and the `usePageMeta` call in each view.

const localePath = useLocalePath()

const { t } = useI18n()
</script>

<template>
  <main class="mx-auto max-w-3xl space-y-8 p-4 sm:p-8">
    <p v-if="loading" class="text-sm text-muted-foreground">{{ t('common.states.loading') }}</p>

    <div v-else-if="notFound" class="space-y-3">
      <!-- Interpolated rather than concatenated: German puts the negation last ("Location nicht
           gefunden"), so the two halves cannot be separate strings. -->
      <h1 class="text-section font-bold tracking-tight">
        {{ t('detail.notFoundHeading', { kind }) }}
      </h1>
      <p class="text-muted-foreground">{{ notFoundText }}</p>
      <Button as-child variant="outline">
        <RouterLink :to="localePath('/events')">{{ t('common.actions.browseEvents') }}</RouterLink>
      </Button>
    </div>

    <p v-else-if="error" class="text-sm text-destructive">{{ error }}</p>

    <template v-else-if="ready">
      <!--
        The same three lengths as the event poster, because this is the same column: `max-w-3xl`
        less `sm:p-8` is 704 px, and the viewport less its padding below that. They are what turn
        `srcset`'s widths into a choice, so they track the `<main>` classes above.

        The wrapper carries the spacing and the caption. `space-y-8` puts its margin on the child
        before the gap, and `CachedImage` renders a `display: contents` <picture> with no box to
        hold one.

        `eager` because this is the largest element above the fold, and a lazy LCP is the defect
        #1207 reports on the event card.
      -->
      <div v-if="imageUrl" class="space-y-2">
        <CachedImage
          :src="imageUrl"
          :sources="imageSources"
          :intrinsic-width="intrinsicWidth"
          :intrinsic-height="intrinsicHeight"
          :alt="name ?? ''"
          loading="eager"
          sizes="(min-width: 768px) 704px, (min-width: 640px) calc(100vw - 4rem), calc(100vw - 2rem)"
          img-class="w-full border border-border object-cover"
        />
        <ImageCreditLine v-if="credit" :credit="credit" />
      </div>

      <!--
        No picture, no placeholder. #811 draws one on a card, where a hole in a grid reads as
        broken, and that reasoning does not reach a detail page: the name is the content, and a
        full-width 3:2 void would push it off the screen for the venues that have no photograph.
      -->
      <header class="space-y-2">
        <SectionLabel as="p">{{ kind }}</SectionLabel>
        <h1 class="text-page font-bold tracking-tight">{{ name }}</h1>
        <slot name="meta" />
      </header>

      <slot />

      <section class="space-y-4">
        <SectionLabel>{{ t('common.upcomingEvents') }}</SectionLabel>
        <p v-if="eventsLoading" class="text-sm text-muted-foreground">
          {{ t('common.states.loading') }}
        </p>
        <p v-else-if="eventsError" class="text-sm text-destructive">{{ eventsError }}</p>
        <p v-else-if="!events?.content?.length" class="text-sm text-muted-foreground">
          {{ emptyText }}
        </p>
        <div v-else :class="CARD_GRID_CLASS">
          <EventCard v-for="event in events.content" :key="event.slug" :event="event" />
        </div>
      </section>

      <!-- Collapsed and content-gated: history must not bury the forecast, and the depth
           caveat on an empty list would read as an excuse. -->
      <details v-if="pastEvents?.content?.length">
        <summary class="cursor-pointer">
          <SectionLabel as="span">{{ t('common.pastEvents') }}</SectionLabel>
        </summary>
        <p class="pt-3 text-sm text-muted-foreground">{{ t('common.pastEventsNote') }}</p>
        <div :class="[CARD_GRID_CLASS, 'pt-3']">
          <EventCard v-for="event in pastEvents.content" :key="event.slug" :event="event" />
        </div>
      </details>
    </template>
  </main>
</template>
