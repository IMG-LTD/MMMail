import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

describe('ShellNavLink', () => {
  it('renders a maturity pill next to the label when provided', async () => {
    const { default: ShellNavLink } = await import('~/components/shell/ShellNavLink.vue')
    const wrapper = mount(ShellNavLink, {
      props: {
        to: '/pass',
        label: 'Pass',
        maturityLabel: 'Beta',
        maturityTone: 'beta',
        dataTestid: 'default-nav-pass'
      },
      global: {
        stubs: {
          NuxtLink: {
            props: ['to'],
            template: '<a :href="to" :data-testid="$attrs[\'data-testid\']"><slot /></a>'
          },
          ElBadge: {
            props: ['value'],
            template: '<span class="badge">{{ value }}</span>'
          }
        }
      }
    })

    const link = wrapper.get('[data-testid="default-nav-pass"]')

    expect(link.attributes('href')).toBe('/pass')
    expect(link.text()).toContain('Pass')
    expect(link.text()).toContain('Beta')
    expect(wrapper.get('.nav-item__maturity').attributes('data-tone')).toBe('beta')
  })

  it('renders count badges independently from maturity labels', async () => {
    const { default: ShellNavLink } = await import('~/components/shell/ShellNavLink.vue')
    const wrapper = mount(ShellNavLink, {
      props: {
        to: '/inbox',
        label: 'Inbox',
        badgeValue: 12
      },
      global: {
        stubs: {
          NuxtLink: {
            props: ['to'],
            template: '<a :href="to"><slot /></a>'
          },
          ElBadge: {
            props: ['value'],
            template: '<span class="badge">{{ value }}</span>'
          }
        }
      }
    })

    expect(wrapper.text()).toContain('Inbox')
    expect(wrapper.text()).toContain('12')
    expect(wrapper.find('.nav-item__maturity').exists()).toBe(false)
  })
})
