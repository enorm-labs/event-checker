<script lang="ts" setup>
/**
 * Third-party open-source attributions.
 *
 * The route and shell exist now; the actual dependency list is **generated** in Phase 5 of
 * docs/FOOTER_AND_LEGAL_PLAN.md §9, because hand-maintaining several hundred transitive
 * dependencies and their licence texts is exactly the kind of thing that is wrong within a week.
 * Until then this page is honest about what it does not yet list, and points at the manifests,
 * which are authoritative and public.
 *
 * This page is deliberately **not linked from the footer yet** — see §11, Phase 5.
 */
import LegalPage from '@/components/LegalPage.vue'
</script>

<template>
  <LegalPage
    intro="Event Junkie is built on open-source software. This page will carry the full attribution list."
    title="Open-source notices"
  >
    <aside class="rounded-lg border border-border bg-muted/50 p-4 text-sm">
      <p class="font-medium text-foreground">The generated list is not here yet.</p>
      <p class="mt-2">
        Attribution is produced from the build rather than typed by hand, so that it stays complete
        and correct as dependencies change. Until that runs, the manifests below are the
        authoritative record of what this project depends on.
      </p>
    </aside>

    <section>
      <h2>Where to look in the meantime</h2>
      <ul>
        <li>
          Frontend dependencies —
          <a
            href="https://github.com/enorm-labs/event-checker/blob/main/events-frontend/package.json"
            rel="noopener"
            target="_blank"
          >
            <code>events-frontend/package.json</code>
          </a>
        </li>
        <li>
          Backend dependencies — the <code>build.gradle.kts</code> files and
          <a
            href="https://github.com/enorm-labs/event-checker/blob/main/gradle.properties"
            rel="noopener"
            target="_blank"
          >
            <code>gradle.properties</code>
          </a>
        </li>
        <li>
          The complete resolved tree, including transitive dependencies —
          <a
            href="https://github.com/enorm-labs/event-checker/network/dependencies"
            rel="noopener"
            target="_blank"
          >
            the repository's dependency graph
          </a>
        </li>
      </ul>
    </section>

    <section>
      <h2>Our own code</h2>
      <p>
        Event Junkie's source is published under the Apache License 2.0 and is available in full at
        <a href="https://github.com/enorm-labs/event-checker" rel="noopener" target="_blank">
          github.com/enorm-labs/event-checker
        </a>
        . Where a dependency ships a <code>NOTICE</code> file, its contents will be reproduced on
        this page, as § 4 (d) of that licence requires.
      </p>
    </section>
  </LegalPage>
</template>
