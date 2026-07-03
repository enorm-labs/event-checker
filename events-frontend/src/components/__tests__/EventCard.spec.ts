import { describe, expect, it } from 'vitest'

import { mount } from '@vue/test-utils'
import EventCard from '@/components/EventCard.vue'
import type { EventSummary } from '@/api/types'
import { todayIso } from '@/lib/format'

const event: EventSummary = {
  slug: 'tonight-show',
  title: 'Tonight Show',
  subtitle: 'with support',
  eventDate: '2026-06-30',
  startTime: '20:00',
  soldOut: true,
  priceCurrency: 'EUR',
  pricePresale: 25,
  genreTags: ['Punk'],
  venue: { slug: 'lido', name: 'Lido', city: 'Berlin' },
}

// Stub RouterLink to a plain anchor so we can assert the target without a full router.
const stubs = {
  RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] },
}

describe('EventCard', () => {
  it('renders the event title and venue name', () => {
    const wrapper = mount(EventCard, { props: { event }, global: { stubs } })
    expect(wrapper.text()).toContain('Tonight Show')
    expect(wrapper.text()).toContain('Lido')
  })

  it('links to the event detail route', () => {
    const wrapper = mount(EventCard, { props: { event }, global: { stubs } })
    expect(wrapper.get('a').attributes('href')).toBe('/events/tonight-show')
  })

  it('shows a sold-out badge when the event is sold out', () => {
    const wrapper = mount(EventCard, { props: { event }, global: { stubs } })
    expect(wrapper.text()).toContain('Sold out')
  })

  it('marks an event happening today as live', () => {
    const wrapper = mount(EventCard, {
      props: { event: { ...event, eventDate: todayIso() } },
      global: { stubs },
    })
    expect(wrapper.text()).toContain('Live tonight')
  })

  it('does not mark an event on another day as live', () => {
    const wrapper = mount(EventCard, {
      props: { event: { ...event, eventDate: '2099-12-31' } },
      global: { stubs },
    })
    expect(wrapper.text()).not.toContain('Live tonight')
  })
})
