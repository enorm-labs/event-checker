import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

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
      // Lazy-loaded so FullCalendar's weight does not affect first paint elsewhere (see ADR-011).
      component: () => import('../views/CalendarView.vue'),
    },
    {
      path: '/events',
      name: 'events',
      component: () => import('../views/EventsView.vue'),
    },
    {
      path: '/events/:slug',
      name: 'event',
      component: () => import('../views/EventDetailView.vue'),
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
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
  ],
})

export default router
