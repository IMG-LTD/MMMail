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

  it('renders start-here checklist and concrete resource links', async () => {
    const panel = await mountPanel()

    expect(panel.get('[data-testid="settings-adoption-checklist-link-mail_e2ee"]').attributes('href')).toBe('/settings#settings-mail-e2ee-panel')
    expect(panel.get('[data-testid="settings-adoption-checklist-link-pwa_web_push"]').attributes('href')).toBe('/settings#settings-pwa-panel')
    expect(panel.get('[data-testid="settings-adoption-checklist-link-boundary_map"]').attributes('href')).toBe('/suite?section=boundary')
    expect(panel.get('[data-testid="settings-adoption-checklist-link-self_hosted_guide"]').attributes('href')).toBe('/self-hosted/install.html')
    expect(panel.get('[data-testid="settings-adoption-checklist-link-secure_share"]').attributes('href')).toBe('/drive')
    expect(panel.get('[data-testid="settings-adoption-api-guide-link"]').attributes('href')).toBe('/self-hosted/developer.html')
    expect(panel.get('[data-testid="settings-adoption-swagger-link"]').attributes('href')).toBe('http://localhost:8080/swagger-ui.html')
    expect(panel.get('[data-testid="settings-adoption-openapi-link"]').attributes('href')).toBe('http://localhost:8080/v3/api-docs')
    expect(panel.get('[data-testid="settings-adoption-track-primary-admin_baseline"]').attributes('href')).toBe('/settings#settings-mail-e2ee-panel')
    expect(panel.get('[data-testid="settings-adoption-track-secondary-admin_baseline"]').attributes('href')).toBe('/suite?section=boundary')
    expect(panel.get('[data-testid="settings-adoption-track-primary-team_handoff"]').attributes('href')).toBe('/suite?section=overview')
    expect(panel.get('[data-testid="settings-adoption-track-secondary-team_handoff"]').attributes('href')).toBe('/self-hosted/team.html')
    expect(panel.get('[data-testid="settings-adoption-track-primary-identity_readiness"]').attributes('href')).toBe('/self-hosted/identity.html')
    expect(panel.get('[data-testid="settings-adoption-track-secondary-identity_readiness"]').attributes('href')).toBe('/self-hosted/team.html')
    expect(panel.get('[data-testid="settings-adoption-track-primary-developer_handoff"]').attributes('href')).toBe('/self-hosted/developer.html')
    expect(panel.get('[data-testid="settings-adoption-track-secondary-developer_handoff"]').attributes('href')).toBe('/self-hosted/api.html?apiBase=http%3A%2F%2Flocalhost%3A8080')
    expect(panel.get('[data-testid="settings-adoption-guide-link"]').attributes('href')).toBe('/self-hosted/adoption.html')
    expect(panel.get('[data-testid="settings-adoption-mainline-link"]').attributes('href')).toBe('/suite?section=overview')
    expect(panel.get('[data-testid="settings-adoption-team-guide-link"]').attributes('href')).toBe('/self-hosted/team.html')
    expect(panel.get('[data-testid="settings-adoption-team-pass-link"]').attributes('href')).toBe('/pass')
    expect(panel.get('[data-testid="settings-adoption-identity-guide-link"]').attributes('href')).toBe('/self-hosted/identity.html')
    expect(panel.get('[data-testid="settings-adoption-identity-team-link"]').attributes('href')).toBe('/self-hosted/team.html')
    expect(panel.get('[data-testid="settings-adoption-install-guide-link"]').attributes('href')).toBe('/self-hosted/install.html')
    expect(panel.get('[data-testid="settings-adoption-runbook-link"]').attributes('href')).toBe('/self-hosted/runbook.html')
    expect(panel.get('[data-testid="settings-adoption-api-meta"]').text()).toContain('settings.adoption.cards.api.meta')
    expect(panel.get('[data-testid="settings-adoption-guide-meta"]').text()).toContain('settings.adoption.cards.adoption.meta')
    expect(panel.get('[data-testid="settings-adoption-team-meta"]').text()).toContain('settings.adoption.cards.team.meta')
    expect(panel.get('[data-testid="settings-adoption-identity-meta"]').text()).toContain('settings.adoption.cards.identity.meta')
  })
})

async function mountPanel() {
  const { default: SettingsAdoptionReadinessPanel } = await import('~/components/settings/SettingsAdoptionReadinessPanel.vue')
  return mount(SettingsAdoptionReadinessPanel, {
    global: {
      stubs: {
        NuxtLink: {
          props: ['to'],
          template: '<a :href="to"><slot /></a>'
        },
        ElAlert: {
          props: ['title', 'description'],
          template: '<div><span>{{ title }}</span><span>{{ description }}</span></div>'
        }
      }
    }
  })
}
