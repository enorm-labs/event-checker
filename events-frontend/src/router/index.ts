import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'
import { h } from 'vue'
import { RouterView } from 'vue-router'
import { formatTitle, setPageTitle } from '../composables/usePageTitle'
import { isLocale, LOCALES, type Locale, rememberLocale, resolveLocale } from '../i18n/locales'
import { setI18nLocale } from '../i18n'
import HomeView from '../views/HomeView.vue'

declare module 'vue-router' {
  interface RouteMeta {
    // Per-view page title. Detail views set their title from loaded data instead (see usePageTitle).
    title?: string
  }
}

/**
 * Pass-through parent for the `/:locale` segment. It renders only its child, so the locale lives
 * in the URL without adding a layout level — the app shell is still App.vue.
 */
const LocaleShell = { render: () => h(RouterView) }

/** The locale of the route being navigated to. Always present on `/:locale/*` routes. */
export function localeOf(route: RouteLocationNormalized): Locale {
  const value = route.params.locale
  return isLocale(value) ? value : resolveLocale()
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      // Path-prefixed locales rather than a stored preference: a locale kept only in storage makes
      // every shared link a coin flip for whoever receives it, and is invisible to crawlers.
      // See docs/adr/ADR-013_LOCALISATION.md §Decision 2.
      // The matcher is built from LOCALES so a locale can never be routable without being
      // published — a `/de` route that renders English would be worse than no `/de` at all.
      path: `/:locale(${LOCALES.join('|')})`,
      component: LocaleShell,
      children: [
        {
          path: '',
          name: 'home',
          component: HomeView,
        },
        {
          path: 'calendar',
          name: 'calendar',
          meta: { title: 'Calendar' },
          // Lazy-loaded so FullCalendar's weight does not affect first paint elsewhere (see ADR-011).
          component: () => import('../views/CalendarView.vue'),
        },
        {
          path: 'events',
          name: 'events',
          meta: { title: 'Events' },
          component: () => import('../views/EventsView.vue'),
        },
        {
          path: 'events/:slug',
          name: 'event',
          component: () => import('../views/EventDetailView.vue'),
        },
        {
          path: 'venues',
          name: 'venues',
          meta: { title: 'Venues' },
          component: () => import('../views/VenuesView.vue'),
        },
        {
          path: 'venues/:slug',
          name: 'venue',
          component: () => import('../views/VenueDetailView.vue'),
        },
        {
          path: 'artists/:slug',
          name: 'artist',
          component: () => import('../views/ArtistDetailView.vue'),
        },
        {
          path: 'promoters/:slug',
          name: 'promoter',
          component: () => import('../views/PromoterDetailView.vue'),
        },
        {
          path: 'about',
          name: 'about',
          meta: { title: 'About' },
          // route level code-splitting
          // this generates a separate chunk (About.[hash].js) for this route
          // which is lazy-loaded when the route is visited.
          component: () => import('../views/AboutView.vue'),
        },
        // Legal pages, nested under /legal/* so later additions (accessibility statement, data
        // sources) have an obvious home. Lazy-loaded like every other non-home route: they are read
        // rarely and should not weigh on first paint.
        {
          path: 'legal/imprint',
          name: 'imprint',
          meta: { title: 'Imprint' },
          component: () => import('../views/legal/ImprintView.vue'),
        },
        {
          path: 'legal/privacy',
          name: 'privacy',
          meta: { title: 'Privacy' },
          component: () => import('../views/legal/PrivacyView.vue'),
        },
        {
          path: 'legal/notices',
          name: 'notices',
          meta: { title: 'Open-source notices' },
          component: () => import('../views/legal/NoticesView.vue'),
        },
      ],
    },
    {
      // Anything without a locale prefix — including `/` — gets one and is redirected.
      //
      // The guard against `already` matters: without it, a path that is prefixed but matches no
      // route (`/en/nonsense`) would fall through to here and be prefixed again, producing
      // `/en/en/nonsense` and then looping. Sending it to that locale's home is a deliberate
      // choice for now; a real 404 view is tracked separately.
      path: '/:pathMatch(.*)*',
      redirect: (to) => {
        const [first] = to.path.split('/').filter(Boolean)
        const already = isLocale(first)
        const locale = already ? first : resolveLocale()
        // `to.path` is `/` for the bare root, which would make `/en/` — a second URL for the same
        // page as `/en`, which is what every in-app link produces. Normalise to the shorter form.
        const rest = to.path === '/' ? '' : to.path
        return {
          path: already ? `/${locale}` : `/${locale}${rest}`,
          query: to.query,
          hash: to.hash,
        }
      },
    },
  ],
  // Legal pages are linked from the footer, so they are always reached from the bottom of a
  // scrolled page; without this the browser keeps the old offset and the imprint opens mid-document.
  scrollBehavior(to) {
    return to.hash ? { el: to.hash, behavior: 'smooth' } : { top: 0 }
  },
})

// Apply the URL's locale before the view renders, so the first paint is already in the right
// language rather than flipping after mount.
router.beforeEach((to) => {
  const locale = localeOf(to)
  setI18nLocale(locale)
  rememberLocale(locale)
})

// Static views get their title from route meta. Detail views override it once their entity loads,
// so their meta is intentionally left unset (falls back to the home title).
router.afterEach((to) => {
  setPageTitle(formatTitle(to.meta.title))
})

export default router
