import { describe, expect, it } from 'vitest'

import { mount } from '@vue/test-utils'
import { Button } from '@/components/ui/button'

describe('Button', () => {
  it('renders slot content', () => {
    const wrapper = mount(Button, { slots: { default: 'Browse calendar' } })
    expect(wrapper.text()).toContain('Browse calendar')
  })

  it('reflects the variant prop', () => {
    const wrapper = mount(Button, {
      props: { variant: 'destructive' },
      slots: { default: 'Delete' },
    })
    expect(wrapper.attributes('data-variant')).toBe('destructive')
  })

  it('renders as a button element by default', () => {
    const wrapper = mount(Button, { slots: { default: 'Go' } })
    expect(wrapper.element.tagName).toBe('BUTTON')
  })
})
