<script setup lang="ts">
import { computed } from 'vue'
import { ImageOff } from '@lucide/vue'
import type { ImageSource } from '@/api/types'

/**
 * An image the API may hold better formats of.
 *
 * The BFF returns `imageUrl` plus an `imageSources` list, best format first, once the environment
 * serves cached images (ADR-019). Given the list this renders a `<picture>`. Given none it renders
 * the plain `<img>` a venue's own URL still gets.
 *
 * `sizes` is the caller's, because only the caller knows the layout. A wrong one silently downloads
 * the wrong file.
 *
 * **With no `src` it draws a placeholder rather than nothing**, so callers pass the URL rather than
 * guarding on it. A card that closes up over a missing picture reads as broken, and that is 10% of
 * the corpus (#811).
 */
const props = defineProps<{
  /** The `src`, and the fallback for a browser that reads none of the offered formats. */
  src?: string | null
  sources?: ImageSource[] | null
  alt: string
  /** How wide the image is drawn, as CSS. For a fixed slot that is a length: `80px`. */
  sizes: string
  /** Goes on the `<img>`, never on the wrapper — see the `contents` note in the template. */
  imgClass?: string
  /**
   * The image's credit, as one line of plain text, for a slot too small to caption (#1275).
   *
   * A detail page renders `ImageCreditLine` instead, which links the author and the licence. A card
   * at 3:2 in a two-column grid has no room for either, and a `title` is what is left.
   */
  title?: string | null
  /**
   * `lazy` everywhere but the one image that is the LCP element on its page.
   *
   * A lazily loaded largest-contentful-paint image is the defect #1207 reports, so a caller that
   * knows its image opens the page says so rather than leaving the browser to discover it late.
   */
  loading?: 'lazy' | 'eager'
  /**
   * A box to reserve, as a Tailwind aspect utility: `aspect-[3/2]`.
   *
   * **Only a fixed slot needs this.** The intrinsic dimensions below reserve the *original's* shape,
   * which is right where the height is free and wrong where the caller crops to a box — a poster
   * card would still jump as a portrait flyer arrived into a landscape slot. Given one, the picture
   * becomes the box and the image fills it; given none, nothing about the old behaviour changes.
   */
  aspect?: string
  /** The original's pixel size. Both or neither — the API reports them together. */
  intrinsicWidth?: number | null
  intrinsicHeight?: number | null
}>()

/**
 * `width` and `height`, or nothing at all.
 *
 * They give the browser the aspect ratio before the bytes arrive, so a `loading="lazy"` image
 * reserves its space instead of reflowing the page when it lands. CSS still decides the drawn size.
 *
 * **One of the pair is as bad as neither**, so this returns an empty object rather than half an
 * answer. The API reports them together, and 16% of the stored images have no dimensions at all —
 * a stock JVM reads neither WebP nor AVIF, so nothing measured them at import (#848).
 */
const dimensions = computed(() =>
  props.intrinsicWidth != null && props.intrinsicHeight != null
    ? { width: props.intrinsicWidth, height: props.intrinsicHeight }
    : {},
)
</script>

<template>
  <!--
    Without an `aspect`, `contents` is load-bearing rather than tidy: it takes the <picture> out of
    the layout entirely, so the <img> stays the flex item its classes were written for. Otherwise
    every caller would have to split its classes across two elements.

    With one, the opposite is wanted: the <picture> becomes the reserved box and the image fills it,
    so it has to be a real element with a size.
  -->
  <picture v-if="src" :class="aspect ? [aspect, 'block w-full overflow-hidden'] : 'contents'">
    <!--
      `contents` dissolves the <picture> box and promotes every child into the caller's flex
      container. The <source>s land there too: zero-width flex items that each still claim a `gap`.
      Unhidden, three formats push the image 48 px right of a placeholder card's.
    -->
    <source
      v-for="source in sources ?? []"
      :key="source.type"
      class="hidden"
      :type="source.type"
      :srcset="source.srcset"
      :sizes="sizes"
    />
    <!-- No `srcset` here: the last <source> is JPEG and matches every browser, so this is the
         fallback for one that does not support <picture> at all.

         `dimensions` is dropped when the caller reserved a box: the two disagree by definition, and
         width/height on an element the box already sizes is a second answer to a settled question. -->
    <img
      v-bind="aspect ? {} : dimensions"
      :src="src"
      :alt="alt"
      :title="title ?? undefined"
      :class="[imgClass, aspect && 'size-full object-cover']"
      :loading="loading ?? 'lazy'"
    />
  </picture>
  <!--
    One placeholder for both kinds of nothing. A viewer gains nothing from telling an image the
    venue never published from one a licence withholds, and a second "withheld" variant would have
    to word a position no venue has actually taken (#811).

    `aspect-[3/2]` only does something where the caller's classes leave the height open, which is
    the detail header. The cards pass `size-20`/`size-24`, where both dimensions are already set and
    an aspect ratio is inert.
  -->
  <div
    v-else
    :class="imgClass"
    class="flex aspect-[3/2] items-center justify-center bg-muted text-muted-foreground"
  >
    <ImageOff aria-hidden="true" class="size-1/4 max-h-12 min-h-4 min-w-4 max-w-12" />
  </div>
</template>
