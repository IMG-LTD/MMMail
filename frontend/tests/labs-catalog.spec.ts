import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import {
  COMMUNITY_V1_CURATED_LABS_MODULES,
  COMMUNITY_V1_LABS_MODULES
} from '../constants/module-maturity'

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string, params?: Record<string, string | number>) => {
      if (!params) {
        return key
      }
      return Object.entries(params).reduce((result, [paramKey, value]) => {
        return result.replace(`{${paramKey}}`, String(value))
      }, key)
    }
  })
}))

describe('labs catalog', () => {
  beforeEach(() => {
    ;(globalThis as typeof globalThis & {
      definePageMeta?: (value: unknown) => void
      useHead?: (value: unknown) => void
      useRuntimeConfig?: () => { public: { enablePreviewModules: boolean } }
    }).definePageMeta = vi.fn()
    ;(globalThis as typeof globalThis & {
      definePageMeta?: (value: unknown) => void
      useHead?: (value: unknown) => void
      useRuntimeConfig?: () => { public: { enablePreviewModules: boolean } }
    }).useHead = vi.fn()
    ;(globalThis as typeof globalThis & {
      definePageMeta?: (value: unknown) => void
      useHead?: (value: unknown) => void
      useRuntimeConfig?: () => { public: { enablePreviewModules: boolean } }
    }).useRuntimeConfig = () => ({
      public: {
        enablePreviewModules: true
      }
    })
  })

  afterEach(() => {
    delete (globalThis as typeof globalThis & { definePageMeta?: unknown }).definePageMeta
    delete (globalThis as typeof globalThis & { useHead?: unknown }).useHead
    delete (globalThis as typeof globalThis & { useRuntimeConfig?: unknown }).useRuntimeConfig
  })

  it('keeps the raw labs registry broader than the curated default catalog', () => {
    expect(COMMUNITY_V1_LABS_MODULES.length).toBeGreaterThan(COMMUNITY_V1_CURATED_LABS_MODULES.length)
    expect(COMMUNITY_V1_CURATED_LABS_MODULES.map(item => item.code)).toEqual([
      'AUTHENTICATOR',
      'SIMPLELOGIN',
      'STANDARD_NOTES'
    ])
  })

  it('renders only curated strategy-adjacent modules on the labs page', async () => {
    const page = await mountPage()
    const text = page.text()

    expect(text).toContain('nav.authenticator')
    expect(text).toContain('nav.simpleLogin')
    expect(text).toContain('nav.standardNotes')

    expect(text).not.toContain('nav.pass')
    expect(text).not.toContain('nav.vpn')
    expect(text).not.toContain('nav.meet')
    expect(text).not.toContain('nav.wallet')
    expect(text).not.toContain('nav.lumo')
    expect(text).not.toContain('nav.collaboration')
    expect(text).not.toContain('nav.commandCenter')
    expect(text).not.toContain('nav.notifications')
  })
})

async function mountPage() {
  const { default: LabsPage } = await import('~/pages/labs.vue')
  return mount(LabsPage, {
    global: {
      stubs: {
        ElTag: {
          template: '<span><slot /></span>'
        },
        ElEmpty: {
          props: ['description'],
          template: '<div>{{ description }}</div>'
        }
      }
    }
  })
}
