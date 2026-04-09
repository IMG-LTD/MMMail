import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import type { SuiteReadinessItem } from '~/types/api'

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

vi.mock('~/utils/suite-operations', () => ({
  riskLevelLabel: (value: string) => value,
  riskTagType: () => 'warning'
}))

describe('SuiteReadinessSecurityPanel', () => {
  it('renders a mainline featured readiness card instead of a wallet-only spotlight', async () => {
    const { default: SuiteReadinessSecurityPanel } = await import('~/components/suite/SuiteReadinessSecurityPanel.vue')
    const panel = mount(SuiteReadinessSecurityPanel, {
      props: {
        loading: false,
        readiness: {
          generatedAt: '2026-04-08T10:00:00',
          overallScore: 61,
          overallRiskLevel: 'HIGH',
          highRiskProductCount: 1,
          criticalRiskProductCount: 0,
          items: [buildReadinessItem()]
        },
        featuredReadinessItem: buildReadinessItem(),
        readinessRiskFilter: 'ALL',
        filteredReadinessItems: [buildReadinessItem()],
        securityPosture: null,
        refreshOperations: async () => {}
      },
      global: {
        stubs: {
          ElAlert: { template: '<div />' },
          ElButton: { template: '<button><slot /></button>' },
          ElTag: { props: ['type'], template: '<span><slot /></span>' },
          ElRadioGroup: { props: ['modelValue'], template: '<div><slot /></div>' },
          ElRadioButton: { props: ['value'], template: '<button><slot /></button>' }
        }
      }
    })

    expect(panel.get('[data-testid="suite-readiness-featured-card"]').text()).toContain('suite.operations.readiness.featured.title')
    expect(panel.text()).toContain('suite.operations.readiness.featured.description:{"product":"Mail"}')
    expect(panel.text()).not.toContain('suite.operations.readiness.wallet.title')
  })
})

function buildReadinessItem(): SuiteReadinessItem {
  return {
    productCode: 'MAIL',
    productName: 'Mail',
    category: 'COMMUNICATION',
    enabledByPlan: true,
    score: 61,
    riskLevel: 'HIGH',
    signals: [
      { key: 'blocked_domain_count', value: 2, note: 'blocked' },
      { key: 'encrypted_send_count', value: 5, note: 'encrypted' }
    ],
    blockers: ['mail blocker'],
    actions: [
      { priority: 'P0', productCode: 'MAIL', action: 'Fix mail', actionCode: 'MAIL_FIX' }
    ]
  }
}
