import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string, params?: Record<string, string>) => {
      if (!params) {
        return key
      }
      return `${key}:${JSON.stringify(params)}`
    }
  })
}))

describe('SettingsAdoptionReadinessPanel', () => {
  beforeEach(() => {
    vi.stubGlobal('useRuntimeConfig', () => ({
      public: {
        apiBase: 'http://localhost:8080'
      }
    }))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.clearAllMocks()
  })

  it('renders concrete API docs and self-hosted guide links', async () => {
    const panel = await mountPanel()

    expect(panel.get('[data-testid="settings-adoption-swagger-link"]').attributes('href')).toBe('http://localhost:8080/swagger-ui.html')
    expect(panel.get('[data-testid="settings-adoption-openapi-link"]').attributes('href')).toBe('http://localhost:8080/v3/api-docs')
    expect(panel.get('[data-testid="settings-adoption-install-guide-link"]').attributes('href')).toBe('/self-hosted/install.html')
    expect(panel.get('[data-testid="settings-adoption-runbook-link"]').attributes('href')).toBe('/self-hosted/runbook.html')
    expect(panel.get('[data-testid="settings-adoption-api-meta"]').text()).toContain('settings.adoption.cards.api.meta')
  })
})

async function mountPanel() {
  const { default: SettingsAdoptionReadinessPanel } = await import('~/components/settings/SettingsAdoptionReadinessPanel.vue')
  return mount(SettingsAdoptionReadinessPanel, {
    global: {
      stubs: {
        ElAlert: {
          props: ['title', 'description'],
          template: '<div><span>{{ title }}</span><span>{{ description }}</span></div>'
        }
      }
    }
  })
}
