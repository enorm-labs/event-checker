import { createRouter, createWebHistory } from 'vue-router'
import { formatTitle, setPageTitle } from '../composables/usePageTitle'
import HomeView from '../views/HomeView.vue'

declare module 'vue-router' {
  interface RouteMeta {
    // Per-view page title. Detail views set their title from loaded data instead (see usePageTitle).
    title?: string
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/calendar',
      name: 'calendar',
      meta: { title: 'Calendar' },
      // Lazy-loaded so FullCalendar's weight does not affect first paint elsewhere (see ADR-011).
      component: () => import('../views/CalendarView.vue'),
    },
    {
      path: '/events',
      name: 'events',
      meta: { title: 'Events' },
      component: () => import('../views/EventsView.vue'),
    },
    {
      path: '/events/:slug',
      name: 'event',
      component: () => import('../views/EventDetailView.vue'),
    },
    {
      path: '/venues',
      name: 'venues',
      meta: { title: 'Venues' },
      component: () => import('../views/VenuesView.vue'),
    },
    {
      path: '/venues/:slug',
      name: 'venue',
      component: () => import('../views/VenueDetailView.vue'),
    },
    {
      path: '/artists/:slug',
      name: 'artist',
      component: () => import('../views/ArtistDetailView.vue'),
    },
    {
      path: '/promoters/:slug',
      name: 'promoter',
      component: () => import('../views/PromoterDetailView.vue'),
    },
    {
      path: '/about',
      name: 'about',
      meta: { title: 'About' },
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
  ],
})

// Static views get their title from route meta. Detail views override it once their
// entity loads, so their meta is intentionally left unset (falls back to the home title).
router.afterEach((to) => {
  setPageTitle(formatTitle(to.meta.title))
})

export default router
