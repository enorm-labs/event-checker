<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { ImageCredit } from '@/lib/imageCredit'

/**
 * The credit under a venue, artist or promoter image (#1275).
 *
 * CC BY and CC BY-SA are usable only with the author named, the licence named and the original
 * linked, so this is part of the licence rather than a courtesy. It renders nothing of its own when
 * there is no credit, because `imageCredit()` returns null for an image that has none.
 *
 * **It sits beside `CachedImage`, not inside it.** That component renders a `display: contents`
 * <picture> with no box of its own, so a caption within it would have nothing to sit under.
 */
defineProps<{ credit: ImageCredit }>()

const { t } = useI18n()
</script>

<template>
  <p class="text-xs text-muted-foreground">
    {{ t('common.imageCredit.photo') }}
    <a :href="credit.sourceUrl" class="underline underline-offset-2" rel="noopener" target="_blank">
      {{ credit.attribution }}
    </a>
    <template v-if="credit.licenceUrl">
      ·
      <a
        :href="credit.licenceUrl"
        class="underline underline-offset-2"
        rel="noopener"
        target="_blank"
      >
        {{ credit.licenceLabel }}
      </a>
    </template>
    <template v-else> · {{ credit.licenceLabel }} </template>
  </p>
</template>
