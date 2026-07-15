import { describe, expect, it } from 'vitest'

import { mount } from '@vue/test-utils'
import VenueCard from '@/components/VenueCard.vue'
import type { VenueSummary } from '@/api/types'

const venue: VenueSummary = {
  slug: 'lido',
  name: 'Lido',
  city: 'Berlin',
  address: 'Cuvrystr. 7',
  district: 'friedrichshain-kreuzberg',
  imageUrl: 'https://example.com/lido.jpg',
}

// Stub RouterLink to a plain anchor so we can assert the target without a full router.
const stubs = {
  RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] },
}

describe('VenueCard', () => {
  it('renders the venue name and address', () => {
    const wrapper = mount(VenueCard, { props: { venue }, global: { stubs } })
    expect(wrapper.text()).toContain('Lido')
    expect(wrapper.text()).toContain('Cuvrystr. 7')
  })

  it('shows the human-readable district label, not the slug', () => {
    const wrapper = mount(VenueCard, { props: { venue }, global: { stubs } })
    expect(wrapper.text()).toContain('Friedrichshain-Kreuzberg')
    expect(wrapper.text()).not.toContain('friedrichshain-kreuzberg')
  })

  it('links to the venue detail route', () => {
    const wrapper = mount(VenueCard, { props: { venue }, global: { stubs } })
    expect(wrapper.get('a').attributes('href')).toBe('/venues/lido')
  })

  it('falls back to the city when address and district are missing', () => {
    const wrapper = mount(VenueCard, {
      props: { venue: { slug: 'x', name: 'Somewhere', city: 'Berlin' } },
      global: { stubs },
    })
    expect(wrapper.text()).toContain('Berlin')
  })
})
