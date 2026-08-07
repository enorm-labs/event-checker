<script lang="ts" setup>
/**
 * Site footer: provenance, project links and the copyright line.
 *
 * Scope note (docs/FOOTER_AND_LEGAL_PLAN.md §11): only links that actually resolve are rendered.
 * "Contributing" arrives with Phase 6 — the plan's own principle is that a footer with dead links
 * is worse than no footer.
 *
 * The disclaimer sits here rather than only on a legal page because it is the single most useful
 * sentence for a user of an aggregator, and nobody clicks through to read it (§7.6).
 */
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import BrandLogo from '@/components/BrandLogo.vue'
import { useAppMeta } from '@/composables/useAppMeta'
import {
  commitUrl,
  LICENSE_URL,
  NEW_ISSUE_URL,
  releaseTagUrl,
  RELEASES_URL,
  REPOSITORY_URL,
} from '@/lib/links'

// Hardcoded rather than `new Date().getFullYear()`: a clock-derived year makes an archived page
// claim a copyright it never carried, and it would make any snapshot test depend on the date (§3).
const COPYRIGHT_YEAR = 2026

const linkClass = 'text-muted-foreground hover:text-foreground'

const { meta } = useAppMeta()

/**
 * A release tag only exists for a clean version. `dev` (no build info — the IDE and `bootRun`) and
 * `-SNAPSHOT` (every build until a release workflow exists, §4.7) render as plain text, because a
 * link to `/releases/tag/v0.1.0-SNAPSHOT` would 404.
 */
const releaseUrl = computed(() => {
  const version = meta.value?.version
  if (!version || version === 'dev' || version.includes('-SNAPSHOT')) return null
  return releaseTagUrl(version)
})
</script>

<template>
  <footer class="mt-16 border-t border-border">
    <div class="mx-auto max-w-5xl px-4 py-10">
      <div class="grid gap-8 sm:grid-cols-2 md:grid-cols-3">
        <div class="space-y-3">
          <BrandLogo always-show-wordmark />
          <p class="text-sm text-muted-foreground">Can't get enough of Berlin</p>
          <!-- "Alle Angaben ohne Gewähr", in the register the brand actually speaks (§7.6). -->
          <p class="max-w-prose text-sm text-muted-foreground">
            Event data is aggregated from public sources and provided without warranty — always
            check with the venue before you go.
          </p>
        </div>

        <nav aria-labelledby="footer-project-heading" class="space-y-3 text-sm">
          <h2 id="footer-project-heading" class="font-medium text-foreground">Project</h2>
          <ul class="space-y-2">
            <li>
              <a :class="linkClass" :href="REPOSITORY_URL" rel="noopener" target="_blank">
                Source on GitHub
              </a>
            </li>
            <li>
              <a :class="linkClass" :href="NEW_ISSUE_URL" rel="noopener" target="_blank">
                Report an issue
              </a>
            </li>
            <li>
              <a :class="linkClass" :href="RELEASES_URL" rel="noopener" target="_blank">
                Changelog
              </a>
            </li>
          </ul>
        </nav>

        <nav aria-labelledby="footer-legal-heading" class="space-y-3 text-sm">
          <h2 id="footer-legal-heading" class="font-medium text-foreground">Legal</h2>
          <ul class="space-y-2">
            <li>
              <RouterLink :class="linkClass" to="/legal/imprint">Imprint</RouterLink>
            </li>
            <li>
              <RouterLink :class="linkClass" to="/legal/privacy">Privacy</RouterLink>
            </li>
            <li>
              <RouterLink :class="linkClass" to="/legal/notices">Open-source notices</RouterLink>
            </li>
          </ul>
        </nav>
      </div>

      <div
        class="mt-8 border-t border-border pt-6 text-sm text-muted-foreground sm:flex sm:items-center sm:justify-between"
      >
        <!-- Two clauses, deliberately: the copyright covers this site's own design and text, the
             licence covers the code. Event data is neither ours to licence nor covered here (§3). -->
        <p>
          © {{ COPYRIGHT_YEAR }} Event Junkie ·
          <a :class="linkClass" :href="LICENSE_URL" rel="noopener" target="_blank">
            Code under Apache-2.0
          </a>
        </p>

        <!-- Renders nothing until /meta resolves. A version is worth exactly one thing — telling
             you what someone was running when they report a bug — so it must never cost a layout
             shift or an error state to obtain (§4.4). -->
        <p v-if="meta?.version" class="mt-4 font-mono text-xs sm:mt-0" data-testid="app-version">
          <component
            :is="releaseUrl ? 'a' : 'span'"
            v-bind="releaseUrl ? { href: releaseUrl, rel: 'noopener', target: '_blank' } : {}"
            :class="releaseUrl ? linkClass : ''"
          >
            v{{ meta.version }}
          </component>
          <template v-if="meta.commit && meta.commitShort">
            ·
            <a
              :class="linkClass"
              :href="commitUrl(meta.commit)"
              :title="`Built from commit ${meta.commit}${meta.buildTime ? ` on ${meta.buildTime}` : ''}`"
              rel="noopener"
              target="_blank"
            >
              {{ meta.commitShort }}
            </a>
          </template>
        </p>
      </div>
    </div>
  </footer>
</template>
