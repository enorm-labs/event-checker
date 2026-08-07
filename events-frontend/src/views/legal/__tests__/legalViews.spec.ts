import { describe, expect, it } from 'vitest'

import { mount } from '@vue/test-utils'
import ImprintView from '@/views/legal/ImprintView.vue'
import PrivacyView from '@/views/legal/PrivacyView.vue'
import { CONTROLLER } from '@/lib/legal'

const stubs = {
  RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] },
}

const mountImprint = () => mount(ImprintView, { global: { stubs } })
const mountPrivacy = () => mount(PrivacyView, { global: { stubs } })

describe('Imprint', () => {
  it('names the provider and a reachable postal address (§ 5 DDG)', () => {
    const text = mountImprint().text()
    expect(text).toContain(CONTROLLER.name)
    expect(text).toContain(CONTROLLER.street)
    expect(text).toContain(CONTROLLER.city)
  })

  it('offers an email address, not only a web form', () => {
    expect(mountImprint().get(`a[href="mailto:${CONTROLLER.email}"]`)).toBeTruthy()
  })

  it('names a person responsible for editorial content (§ 18 (2) MStV)', () => {
    expect(mountImprint().text()).toContain('§ 18 (2) MStV')
  })

  it('carries the disclaimer in its formal register', () => {
    const text = mountImprint().text()
    expect(text).toContain('without warranty as to accuracy, completeness or timeliness')
    expect(text).toContain('Liability for links')
  })

  it('separates our code licence from third-party rights in the event data', () => {
    const text = mountImprint().text()
    expect(text).toContain('Apache License 2.0')
    expect(text).toContain('remain the property of their respective rights holders')
  })
})

describe('Privacy', () => {
  // Art. 13 has twelve mandatory elements (docs/FOOTER_AND_LEGAL_PLAN.md §7.2); omitting one is
  // the usual defect, and it is invisible without a checklist. This is that checklist.
  const required: [string, RegExp][] = [
    ['controller identity', new RegExp(CONTROLLER.name)],
    ['controller contact', new RegExp(CONTROLLER.email)],
    ['absence of a DPO', /no data protection officer/i],
    ['legal basis', /Art\. 6 \(1\) \(f\) GDPR/],
    ['legitimate interests, spelled out', /legitimate interest is operating/i],
    ['recipients', /Hetzner|Cloudflare/],
    ['third-country transfer', /US company/],
    ['retention period', /deleted after seven days/i],
    ['right of access', /Art\. 15/],
    ['right to erasure', /Art\. 17/],
    ['right to object, prominently', /Right to object \(Art\. 21 GDPR\)/],
    ['right to complain to a supervisory authority', /Berliner Beauftragte/],
    ['whether provision is required', /neither legally nor contractually obliged/i],
    ['no automated decision-making', /Art\. 22 GDPR/],
  ]

  for (const [element, pattern] of required) {
    it(`states the ${element}`, () => {
      expect(mountPrivacy().text()).toMatch(pattern)
    })
  }

  it('describes the local-storage key and why it needs no consent', () => {
    const text = mountPrivacy().text()
    expect(text).toContain('§ 25 (2) 2 TDDDG')
    expect(text).toMatch(/no cookies/i)
  })

  it('names artist data as personal data and offers a removal route', () => {
    const text = mountPrivacy().text()
    expect(text).toMatch(/artist is a natural person/i)
    expect(text).toMatch(/removed or corrected/i)
  })

  it('does not describe processing that does not happen', () => {
    // A notice claiming cookie consent, analytics or ad partners we do not have is as inaccurate
    // as one omitting processing we do — and generators produce exactly that (§7.8).
    const text = mountPrivacy().text()
    expect(text).not.toMatch(/Google Analytics|advertising partners|withdraw your cookie consent/i)
  })

  it('carries the same controller address as the imprint', () => {
    // §8.3: the two pages must never disagree about the address. They share one module, and this
    // asserts the sharing actually reaches the rendered output.
    const privacy = mountPrivacy().text()
    const imprint = mountImprint().text()
    for (const line of [CONTROLLER.name, CONTROLLER.street, CONTROLLER.city]) {
      expect(privacy).toContain(line)
      expect(imprint).toContain(line)
    }
  })
})

describe('while the pages are provisional', () => {
  it('says so rather than presenting placeholders as fact', () => {
    for (const text of [mountImprint().text(), mountPrivacy().text()]) {
      expect(text).toContain('This page is not final')
      expect(text).toMatch(/placeholders/i)
    }
  })
})
