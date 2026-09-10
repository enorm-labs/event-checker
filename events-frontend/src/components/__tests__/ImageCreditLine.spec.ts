import { describe, expect, it } from 'vitest'

import { mount } from '@vue/test-utils'
import ImageCreditLine from '@/components/ImageCreditLine.vue'

/**
 * What a CC BY or CC BY-SA image obliges the page to show (#1275).
 *
 * The author, the licence and a link back to the original. A caption that names the author and
 * stops there is the failure worth an assertion, because it looks finished.
 */
describe('ImageCreditLine', () => {
  const credit = {
    attribution: 'Photographer Name, via Wikimedia Commons',
    sourceUrl: 'https://commons.wikimedia.org/wiki/File:Example.jpg',
    licenceLabel: 'CC BY-SA 4.0',
    licenceUrl: 'https://creativecommons.org/licenses/by-sa/4.0/',
  }

  it('names the author and links both the original and the licence deed', () => {
    const wrapper = mount(ImageCreditLine, { props: { credit } })
    const hrefs = wrapper.findAll('a').map((a) => a.attributes('href'))

    expect(wrapper.text()).toContain('Photographer Name, via Wikimedia Commons')
    expect(hrefs).toEqual([
      'https://commons.wikimedia.org/wiki/File:Example.jpg',
      'https://creativecommons.org/licenses/by-sa/4.0/',
    ])
  })

  it('still names a public-domain licence, with no deed to link', () => {
    const wrapper = mount(ImageCreditLine, {
      props: { credit: { ...credit, licenceLabel: 'Public domain', licenceUrl: null } },
    })

    expect(wrapper.text()).toContain('Public domain')
    expect(wrapper.findAll('a')).toHaveLength(1)
  })
})
