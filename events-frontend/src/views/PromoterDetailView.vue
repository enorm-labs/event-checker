<script lang="ts" setup>
import { computed, onMounted, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { Button } from '@/components/ui/button'
import EventCard from '@/components/EventCard.vue'
import SectionLabel from '@/components/SectionLabel.vue'
import { usePromoter } from '@/composables/usePromoter'
import { usePageTitle } from '@/composables/usePageTitle'
import { useEventSearch } from '@/composables/useEvents'

const route = useRoute()
const slug = computed(() => String(route.params.slug))

const {
  data: promoter,
  error,
  notFound,
  loading,
  run: loadPromoter,
} = usePromoter(() => slug.value)

usePageTitle(() => (notFound.value ? 'Promoter not found' : (promoter.value?.name ?? 'Promoter')))
const {
  data: events,
  error: eventsError,
  loading: eventsLoading,
  run: loadEvents,
} = useEventSearch(() => ({ promoter: slug.value, size: 50 }), 'events from this promoter')

function reload() {
  loadPromoter()
  loadEvents()
}

onMounted(reload)
watch(slug, reload)
</script>

<template>
  <main class="mx-auto max-w-3xl space-y-8 p-8">
    <p v-if="loading" class="text-sm text-muted-foreground">Cueing it up…</p>

    <div v-else-if="notFound" class="space-y-3">
      <h1 class="text-2xl font-bold tracking-tight">Promoter not found</h1>
      <p class="text-muted-foreground">This promoter isn't in our books.</p>
      <Button as-child variant="outline">
        <RouterLink to="/events">Browse events</RouterLink>
      </Button>
    </div>

    <p v-else-if="error" class="text-sm text-destructive">{{ error }}</p>

    <template v-else-if="promoter">
      <header class="flex gap-4">
        <img
          v-if="promoter.imageUrl"
          :alt="promoter.name ?? ''"
          :src="promoter.imageUrl"
          class="size-24 shrink-0 rounded-lg border border-border object-cover"
          loading="lazy"
        />
        <div class="space-y-1">
          <SectionLabel as="p">Promoter</SectionLabel>
          <h1 class="text-3xl font-bold tracking-tight">{{ promoter.name }}</h1>
          <a
            v-if="promoter.websiteUrl"
            :href="promoter.websiteUrl"
            class="text-sm text-primary underline-offset-4 hover:underline"
            rel="noopener noreferrer"
            target="_blank"
          >
            Website
          </a>
        </div>
      </header>

      <section class="space-y-4">
        <SectionLabel>Upcoming events</SectionLabel>
        <p v-if="eventsLoading" class="text-sm text-muted-foreground">Cueing it up…</p>
        <p v-else-if="eventsError" class="text-sm text-destructive">{{ eventsError }}</p>
        <p v-else-if="!events?.content?.length" class="text-sm text-muted-foreground">
          Nothing on their calendar yet — check back soon.
        </p>
        <div v-else class="grid gap-3 sm:grid-cols-2">
          <EventCard v-for="event in events.content" :key="event.slug" :event="event" />
        </div>
      </section>
    </template>
  </main>
</template>
