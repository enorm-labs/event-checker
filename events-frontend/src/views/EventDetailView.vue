<script lang="ts" setup>
import { computed, onMounted, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { Button } from '@/components/ui/button'
import { useEvent } from '@/composables/useEvent'
import { formatDate, formatPrice, formatTime } from '@/lib/format'

const route = useRoute()
const slug = computed(() => String(route.params.slug))

const { data: event, error, notFound, loading, run } = useEvent(() => slug.value)

// Lineup arrives in billing order already, but sort defensively so headliners stay first.
const lineup = computed(() =>
  [...(event.value?.lineup ?? [])].sort((a, b) => (a.billingOrder ?? 0) - (b.billingOrder ?? 0)),
)

const roleLabels: Record<string, string> = {
  HEADLINER: 'Headliner',
  SUPPORT: 'Support',
  DJ: 'DJ',
}

onMounted(run)
watch(slug, run)
</script>

<template>
  <main class="mx-auto max-w-3xl space-y-8 p-8">
    <p v-if="loading" class="text-sm text-muted-foreground">Loading…</p>

    <div v-else-if="notFound" class="space-y-3">
      <h1 class="text-2xl font-bold tracking-tight">Event not found</h1>
      <p class="text-muted-foreground">This event may have been removed or never existed.</p>
      <Button as-child variant="outline">
        <RouterLink to="/">Back to home</RouterLink>
      </Button>
    </div>

    <p v-else-if="error" class="text-sm text-destructive">{{ error }}</p>

    <article v-else-if="event" class="space-y-8">
      <header class="space-y-3">
        <h1 class="text-3xl font-bold tracking-tight">{{ event.title }}</h1>
        <p v-if="event.subtitle" class="text-lg text-muted-foreground">{{ event.subtitle }}</p>
        <div class="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
          <span>{{ formatDate(event.eventDate) }}</span>
          <span v-if="event.startTime">· {{ formatTime(event.startTime) }}</span>
          <span v-if="event.venue?.name">· {{ event.venue.name }}</span>
          <span
            v-if="event.status && event.status !== 'SCHEDULED'"
            class="rounded-full bg-destructive/10 px-2 py-0.5 text-xs font-medium text-destructive"
          >
            {{ event.status }}
          </span>
          <span
            v-if="event.soldOut"
            class="rounded-full bg-destructive/10 px-2 py-0.5 text-xs font-medium text-destructive"
          >
            Sold out
          </span>
        </div>
      </header>

      <img
        v-if="event.imageUrl"
        :src="event.imageUrl"
        :alt="event.title ?? ''"
        class="w-full rounded-lg border border-border object-cover"
        loading="lazy"
      />

      <p v-if="event.description" class="whitespace-pre-line text-foreground/90">
        {{ event.description }}
      </p>

      <section v-if="lineup.length" class="space-y-3">
        <h2 class="text-xl font-semibold tracking-tight">Lineup</h2>
        <ul class="space-y-2">
          <li
            v-for="entry in lineup"
            :key="entry.artist?.slug ?? entry.artist?.name"
            class="flex items-center justify-between gap-3 rounded-lg border border-border p-3"
          >
            <RouterLink
              v-if="entry.artist?.slug"
              :to="`/artists/${entry.artist.slug}`"
              class="font-medium text-primary underline-offset-4 hover:underline"
            >
              {{ entry.artist.name }}
            </RouterLink>
            <span v-else class="font-medium">{{ entry.artist?.name }}</span>
            <span v-if="entry.role" class="text-xs text-muted-foreground">
              {{ roleLabels[entry.role] ?? entry.role }}
            </span>
          </li>
        </ul>
      </section>

      <section class="grid gap-6 sm:grid-cols-2">
        <div v-if="event.venue" class="space-y-1">
          <h2 class="text-sm font-medium text-muted-foreground">Venue</h2>
          <RouterLink
            v-if="event.venue.slug"
            :to="`/venues/${event.venue.slug}`"
            class="font-medium text-primary underline-offset-4 hover:underline"
          >
            {{ event.venue.name }}
          </RouterLink>
          <p v-else class="font-medium">{{ event.venue.name }}</p>
          <p v-if="event.venue.address" class="text-sm text-muted-foreground">
            {{ event.venue.address
            }}<template v-if="event.venue.city">, {{ event.venue.city }}</template>
          </p>
        </div>

        <div
          v-if="
            formatPrice(event.pricePresale, event.priceCurrency) ||
            formatPrice(event.priceBoxOffice, event.priceCurrency) ||
            event.priceNote
          "
          class="space-y-1"
        >
          <h2 class="text-sm font-medium text-muted-foreground">Tickets</h2>
          <p v-if="formatPrice(event.pricePresale, event.priceCurrency)" class="text-sm">
            Presale: {{ formatPrice(event.pricePresale, event.priceCurrency) }}
          </p>
          <p v-if="formatPrice(event.priceBoxOffice, event.priceCurrency)" class="text-sm">
            Box office: {{ formatPrice(event.priceBoxOffice, event.priceCurrency) }}
          </p>
          <p v-if="event.priceNote" class="text-sm text-muted-foreground">{{ event.priceNote }}</p>
        </div>
      </section>

      <section v-if="event.promoters?.length" class="space-y-1">
        <h2 class="text-sm font-medium text-muted-foreground">Promoters</h2>
        <p class="text-sm">{{ event.promoters.map((p) => p.name).join(', ') }}</p>
      </section>

      <section
        v-if="event.ticketUrl || event.sourceUrl || event.facebookEventUrl"
        class="flex flex-wrap gap-3"
      >
        <Button v-if="event.ticketUrl" as-child>
          <a :href="event.ticketUrl" rel="noopener noreferrer" target="_blank">Buy tickets</a>
        </Button>
        <Button v-if="event.sourceUrl" as-child variant="outline">
          <a :href="event.sourceUrl" rel="noopener noreferrer" target="_blank">Event page</a>
        </Button>
        <Button v-if="event.facebookEventUrl" as-child variant="outline">
          <a :href="event.facebookEventUrl" rel="noopener noreferrer" target="_blank">Facebook</a>
        </Button>
      </section>
    </article>
  </main>
</template>
