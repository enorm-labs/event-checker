<script lang="ts" setup>
/**
 * „Über uns" — deutsche Fassung von `AboutView.en.vue`.
 *
 * Written rather than translated. The beta section is brand voice, not neutral prose
 * (docs/LOCALISATION_PLAN.md §5), and a literal rendering of *"still finding its feet"* or
 * *"the nights worth showing up for"* reads as a machine translation of an English site — which
 * is the one thing the page is there to disprove.
 */
import { RouterLink } from 'vue-router'
import { useLocalePath } from '@/composables/useLocalePath'

const localePath = useLocalePath()
</script>

<template>
  <main class="mx-auto max-w-3xl space-y-4 p-8">
    <h1 class="text-3xl font-bold tracking-tight">Über uns</h1>
    <p class="text-muted-foreground">
      Event Junkie ist dein Überblick über das, was in Berlins Locations läuft — ein einziger, immer
      frischer Feed mit Live-Events, von Konzerten und Clubnächten über Festivals und Partys bis zum
      gelegentlichen Kneipenquiz.
    </p>
    <p class="text-muted-foreground">
      Verschaff dir einen Überblick, filtere nach Genre oder Art, um deine Szene zu finden, und öffne
      den Kalender, um deine Woche zu planen. Jedes Event führt weiter zu seiner Location, zum
      Line-up und zu den Künstlerinnen und Künstlern — damit du in die Details gehen kannst und die
      Nächte nicht verpasst, bei denen es sich lohnt aufzutauchen.
    </p>
    <p class="text-muted-foreground">
      Im Fokus steht Musik in jeder Ausprägung — große Bühnen und kleine Hinterzimmer gleichermaßen,
      nicht nur Techno — überall in Berlin.
    </p>

    <!-- Das Beta-Abzeichen im Header verlinkt hierher. `scroll-mt` hält die Überschrift frei vom
         Header, wenn der Anker angesprungen wird. Die id muss `beta` bleiben: sie ist in beiden
         Sprachfassungen dasselbe Ziel. -->
    <section id="beta" class="scroll-mt-8 space-y-4 pt-4">
      <h2 class="text-2xl font-bold tracking-tight">Warum da beta steht</h2>
      <p class="text-muted-foreground">
        Event Junkie ist noch jung. Es funktioniert, wir nutzen es selbst, und es ist wirklich
        nützlich — aber es sortiert sich noch, und das solltest du wissen, bevor du einen Abend
        darauf aufbaust.
      </p>
      <p class="text-muted-foreground">
        <strong class="font-medium text-foreground">Die Abdeckung ist unvollständig.</strong> Neue
        Locations kommen nach und nach dazu; ein ruhiger Abend auf Event Junkie ist also nicht immer
        ein ruhiger Abend in Berlin.
        <strong class="font-medium text-foreground">Details können falsch oder veraltet sein.</strong>
        Events werden automatisch von den Websites der Locations gelesen; wenn eine Show verlegt
        wird, ausverkauft ist oder ausfällt, erfahren wir das beim nächsten Durchlauf, nicht sofort.
        Frag im Zweifel bei der Location nach, bevor du losziehst.
        <strong class="font-medium text-foreground">Dinge ändern sich ohne Ankündigung</strong> —
        Seiten, Filter und die Daten dahinter sind alle noch in Bewegung.
      </p>
      <p class="text-muted-foreground">
        Was beta <em>nicht</em> heißt: Nichts hier ist eine Testphase, für die du später zur Kasse
        gebeten wirst, es werden keine Daten über dich verkauft oder weitergegeben, und nichts trackt
        dich — siehe die
        <RouterLink
          class="text-foreground underline underline-offset-4"
          :to="localePath('/legal/privacy')"
        >
          Datenschutzseite
        </RouterLink>
        , die genau deshalb kurz ist.
      </p>
      <p class="text-muted-foreground">
        Etwas gefunden, das nicht stimmt? Uns Bescheid zu geben ist der schnellste Weg zur Korrektur
        —
        <a
          class="text-foreground underline underline-offset-4"
          href="https://github.com/enorm-labs/event-checker/issues/new/choose"
          rel="noopener"
          target="_blank"
        >
          Issue eröffnen
        </a>
        . Was sich zuletzt geändert hat, steht auf der
        <a
          class="text-foreground underline underline-offset-4"
          href="https://github.com/enorm-labs/event-checker/releases"
          rel="noopener"
          target="_blank"
        >
          Releases-Seite
        </a>
        .
      </p>
    </section>
  </main>
</template>
