<script lang="ts" setup>
/**
 * The shared event filter bar, used by both the events list and the calendar.
 *
 * Every control writes straight to the URL query via `useEventFilters`, and reads its current
 * value back from there — so the component holds no filter state of its own and the two views
 * stay in sync with the address bar without prop or event plumbing. The only local state is the
 * two free-text drafts (search box, price range) that are applied on submit rather than on every
 * keystroke; selects and checkboxes apply immediately.
 */
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Button } from '@/components/ui/button'
import { useEventFilters } from '@/composables/useEventFilters'
import { useGenres } from '@/composables/useGenres'
import { useAllVenues } from '@/composables/useVenues'
import { DATE_PRESETS, type DateRange } from '@/lib/dateRanges'
import { DISTRICTS } from '@/lib/districts'
import { formatEventType, todayIso } from '@/lib/format'

const EVENT_TYPES = [
  'CONCERT',
  'FESTIVAL',
  'PARTY',
  'QUIZ',
  'CLUB_NIGHT',
  'SHOW',
  'SCREENING',
  'EXHIBITION',
  'READING',
  'OTHER',
]

withDefaults(defineProps<{ showDateRange?: boolean }>(), { showDateRange: true })

const route = useRoute()
const { queryString, applyFilters } = useEventFilters()

/** Lower bound for the date pickers: the app is about upcoming events, so past dates are out. */
const today = todayIso()

/**
 * Opens the browser's calendar on a click anywhere in the field. Without this, Chrome only
 * opens it from the calendar icon and a click on the text just moves between date segments.
 * `showPicker` is absent on older browsers, where the icon still works — hence the optional call.
 */
function openDatePicker(event: MouseEvent) {
  const input = event.currentTarget as HTMLInputElement & { showPicker?: () => void }
  input.showPicker?.()
}

/**
 * A preset is just the two date bounds, so it stays in the URL like every other filter and a
 * preset link is shareable. Clicking the active one clears the range again, which is the only
 * way back to "any date" without emptying both inputs by hand.
 */
function togglePreset(range: DateRange) {
  const active = isPresetActive(range)
  applyFilters({ from: active ? '' : range.from, to: active ? '' : range.to })
}

/** True when the URL's range is exactly this preset — it renders as the pressed button. */
function isPresetActive(range: DateRange): boolean {
  return queryString('from') === range.from && queryString('to') === range.to
}

const genres = useGenres()
const venues = useAllVenues()

// Drafts are seeded from the URL and re-synced whenever it changes elsewhere (back/forward,
// a link with filters, another control resetting the query).
const search = ref(queryString('q'))
watch(
  () => route.query.q,
  () => {
    search.value = queryString('q')
  },
)

const minPrice = ref(queryString('minPrice'))
const maxPrice = ref(queryString('maxPrice'))
watch(
  () => [route.query.minPrice, route.query.maxPrice],
  () => {
    minPrice.value = queryString('minPrice')
    maxPrice.value = queryString('maxPrice')
  },
)

onMounted(() => {
  genres.run()
  venues.run()
})
</script>

<template>
  <div class="flex flex-wrap items-end gap-3 rounded-xl border border-border bg-card p-4">
    <form class="flex gap-2" @submit.prevent="applyFilters({ q: search })">
      <input
        v-model="search"
        class="h-8 rounded-lg border border-border bg-background px-3 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        placeholder="Search events…"
        type="search"
      />
      <Button type="submit" variant="outline">Search</Button>
    </form>

    <!--
      Two native date inputs rather than a range-picker component: the browser supplies the
      calendar, the value is already the ISO `YYYY-MM-DD` the BFF wants, and `min`/`max` express
      "not in the past" and "to cannot precede from" without any code. They apply on change like
      the selects, so the bar keeps a single Apply button (the price range's).
      `color-scheme` is what makes the browser's own calendar follow our dark mode.
    -->
    <div v-if="showDateRange" class="flex flex-wrap items-center gap-2">
      <input
        :max="queryString('to') || undefined"
        :min="today"
        :value="queryString('from')"
        aria-label="Earliest event date"
        class="h-8 rounded-lg border border-border bg-background px-2 text-sm outline-none [color-scheme:light] focus-visible:ring-3 focus-visible:ring-ring/50 dark:[color-scheme:dark]"
        type="date"
        @change="applyFilters({ from: ($event.target as HTMLInputElement).value })"
        @click="openDatePicker"
      />
      <span class="text-sm text-muted-foreground">–</span>
      <input
        :min="queryString('from') || today"
        :value="queryString('to')"
        aria-label="Latest event date"
        class="h-8 rounded-lg border border-border bg-background px-2 text-sm outline-none [color-scheme:light] focus-visible:ring-3 focus-visible:ring-ring/50 dark:[color-scheme:dark]"
        type="date"
        @change="applyFilters({ to: ($event.target as HTMLInputElement).value })"
        @click="openDatePicker"
      />

      <!--
        Shortcuts for the ranges people actually ask for. They only set the same from/to the
        inputs do, so the two stay consistent and a preset is as shareable as any other filter.
      -->
      <Button
        v-for="preset in DATE_PRESETS"
        :key="preset.label"
        :aria-pressed="isPresetActive(preset.range())"
        :variant="isPresetActive(preset.range()) ? 'default' : 'outline'"
        size="sm"
        type="button"
        @click="togglePreset(preset.range())"
      >
        {{ preset.label }}
      </Button>
    </div>

    <select
      :value="queryString('eventType')"
      aria-label="Filter by event type"
      class="h-8 rounded-lg border border-border bg-background px-2 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
      @change="applyFilters({ eventType: ($event.target as HTMLSelectElement).value })"
    >
      <option value="">All types</option>
      <!--
        The option value stays the raw enum the BFF filters on; only the label is humanised,
        through the same helper the event cards use — so picking "Club night" here and reading
        it off a card are the same words.
      -->
      <option v-for="type in EVENT_TYPES" :key="type" :value="type">
        {{ formatEventType(type) }}
      </option>
    </select>

    <select
      :value="queryString('venue')"
      aria-label="Filter by venue"
      class="h-8 rounded-lg border border-border bg-background px-2 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
      @change="applyFilters({ venue: ($event.target as HTMLSelectElement).value })"
    >
      <option value="">All venues</option>
      <option v-for="v in venues.data.value ?? []" :key="v.slug" :value="v.slug ?? ''">
        {{ v.name }}
      </option>
    </select>

    <select
      :value="queryString('district')"
      aria-label="Filter by district"
      class="h-8 rounded-lg border border-border bg-background px-2 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
      @change="applyFilters({ district: ($event.target as HTMLSelectElement).value })"
    >
      <option value="">All districts</option>
      <option v-for="d in DISTRICTS" :key="d.slug" :value="d.slug">{{ d.label }}</option>
    </select>

    <select
      :value="queryString('genre')"
      aria-label="Filter by genre"
      class="h-8 rounded-lg border border-border bg-background px-2 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
      @change="applyFilters({ genre: ($event.target as HTMLSelectElement).value })"
    >
      <option value="">All genres</option>
      <option v-for="tag in genres.data.value ?? []" :key="tag.slug" :value="tag.slug ?? ''">
        {{ tag.name }}
      </option>
    </select>

    <form class="flex items-center gap-2" @submit.prevent="applyFilters({ minPrice, maxPrice })">
      <input
        v-model="minPrice"
        aria-label="Minimum presale price"
        class="h-8 w-20 rounded-lg border border-border bg-background px-2 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        inputmode="decimal"
        min="0"
        placeholder="Min €"
        step="0.01"
        type="number"
      />
      <span class="text-sm text-muted-foreground">–</span>
      <input
        v-model="maxPrice"
        aria-label="Maximum presale price"
        class="h-8 w-20 rounded-lg border border-border bg-background px-2 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        inputmode="decimal"
        min="0"
        placeholder="Max €"
        step="0.01"
        type="number"
      />
      <Button type="submit" variant="outline">Apply</Button>
    </form>

    <label class="flex h-8 items-center gap-2 text-sm text-muted-foreground">
      <input
        :checked="queryString('excludeSoldOut') === 'true'"
        class="size-4 rounded border-border accent-primary outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        type="checkbox"
        @change="
          applyFilters({
            excludeSoldOut: ($event.target as HTMLInputElement).checked ? 'true' : '',
          })
        "
      />
      Hide sold out
    </label>

    <label class="flex h-8 items-center gap-2 text-sm text-muted-foreground">
      <input
        :checked="queryString('free') === 'true'"
        class="size-4 rounded border-border accent-primary outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        type="checkbox"
        @change="applyFilters({ free: ($event.target as HTMLInputElement).checked ? 'true' : '' })"
      />
      Free only
    </label>
  </div>
</template>
