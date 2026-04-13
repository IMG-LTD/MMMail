import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string) => ({
      'shell.brand.subtitle': 'Private suite workspace',
      'shell.auth.badge': 'Swiss-inspired suite shell',
      'shell.auth.title': 'One account across Mail, Drive, Docs, Pass, VPN, and more.',
      'shell.auth.subtitle': 'Set your interface language before sign in, then keep the same choice after refresh.',
      'shell.auth.products': 'Mail · Calendar · Drive · Docs · Sheets · Pass · VPN · Wallet · Meet',
    }[key] ?? key),
  }),
}))

async function mountBlankLayout(path: string) {
  vi.resetModules()
  vi.stubGlobal('useRoute', () => ({ path }))
  const { default: BlankLayout } = await import('~/layouts/blank.vue')
  return mount(BlankLayout, {
    slots: {
      default: '<div data-testid="slot-content">content</div>',
    },
    global: {
      stubs: {
        NuxtLink: {
          props: ['to'],
          template: '<a :href="to" data-testid="brand-link"><slot /></a>',
        },
        LocaleSwitcher: true,
      },
    },
  })
}

describe('blank layout', () => {
  beforeEach(() => {
    vi.unstubAllGlobals()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('keeps the public home free of auth rail chrome', async () => {
    const wrapper = await mountBlankLayout('/')

    expect(wrapper.get('[data-testid="brand-link"]').attributes('href')).toBe('/')
    expect(wrapper.find('.auth-rail').exists()).toBe(false)
    expect(wrapper.get('.blank-main').classes()).toContain('blank-main--marketing')
  })

  it('keeps the public boundary page free of auth rail chrome', async () => {
    const wrapper = await mountBlankLayout('/boundary')

    expect(wrapper.get('[data-testid="brand-link"]').attributes('href')).toBe('/')
    expect(wrapper.find('.auth-rail').exists()).toBe(false)
    expect(wrapper.get('.blank-main').classes()).toContain('blank-main--marketing')
  })

  it('keeps auth pages on the auth-focused shell', async () => {
    const wrapper = await mountBlankLayout('/login')

    expect(wrapper.get('[data-testid="brand-link"]').attributes('href')).toBe('/login')
    expect(wrapper.find('.auth-rail').exists()).toBe(true)
    expect(wrapper.get('.blank-main').classes()).not.toContain('blank-main--marketing')
  })
})
