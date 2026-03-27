import { describe, expect, it } from 'vitest'
import { messages } from '../locales'
import type { SuitePlan, SuiteSubscription } from '../types/suite-lumo'
import { translate } from '../utils/i18n'
import {
  buildSuitePlanCatalogSections,
  buildSuiteJourneyQuotaRows,
  buildSuitePlanUsageRows,
  buildSuiteUpgradeSummary,
  formatUsageValue,
  suiteProductStatusTagType,
  usagePercent
} from '../utils/suite-plans'

const plans: SuitePlan[] = [
  {
    code: 'FREE',
    name: 'Free',
    description: 'free',
    segment: 'CONSUMER',
    priceMode: 'FREE',
    priceValue: null,
    recommended: false,
    highlights: ['Starter plan'],
    upgradeTargets: ['UNLIMITED'],
    mailDailySendLimit: 100,
    contactLimit: 200,
    calendarEventLimit: 300,
    calendarShareLimit: 20,
    driveStorageMb: 512,
    enabledProducts: ['MAIL', 'DRIVE', 'PASS']
  },
  {
    code: 'UNLIMITED',
    name: 'Unlimited',
    description: 'unlimited',
    segment: 'CONSUMER',
    priceMode: 'FROM',
    priceValue: '€9.99',
    recommended: true,
    highlights: ['All apps'],
    upgradeTargets: [],
    mailDailySendLimit: 1000,
    contactLimit: 2000,
    calendarEventLimit: 5000,
    calendarShareLimit: 120,
    driveStorageMb: 20480,
    enabledProducts: ['MAIL', 'DRIVE', 'PASS', 'VPN', 'LUMO']
  }
]

const subscription: SuiteSubscription = {
  planCode: 'FREE',
  planName: 'Free',
  status: 'ACTIVE',
  updatedAt: '2026-03-12T10:00:00',
  usage: {
    mailCount: 18,
    contactCount: 9,
    calendarEventCount: 12,
    calendarShareCount: 4,
    driveFileCount: 20,
    driveFolderCount: 6,
    driveStorageBytes: 1024
  },
  plan: plans[0]
}

const visibleProducts = [
  { code: 'MAIL', name: 'Mail', enabledByPlan: true },
  { code: 'DRIVE', name: 'Drive', enabledByPlan: true },
  { code: 'VPN', name: 'VPN', enabledByPlan: false }
]

describe('suite plans helpers', () => {
  it('builds localized usage rows, quota rows, and upgrade summary', () => {
    expect(buildSuitePlanUsageRows(subscription, visibleProducts)).toEqual([
      { key: 'mail', count: 18, limit: 100, unit: 'count' },
      { key: 'contacts', count: 9, limit: 200, unit: 'count' },
      { key: 'driveStorage', count: 1024, limit: 536870912, unit: 'bytes' }
    ])

    expect(buildSuiteJourneyQuotaRows(plans[1], visibleProducts)).toEqual([
      { key: 'mailDailySend', value: 1000 },
      { key: 'contacts', value: 2000 },
      { key: 'driveStorage', value: '20480 MB' }
    ])

    expect(buildSuiteUpgradeSummary(subscription, plans, visibleProducts)).toEqual({
      enabledProductCount: 2,
      totalProductCount: 3,
      availableUpgradeCodes: ['UNLIMITED'],
      availableUpgradeNames: ['Unlimited']
    })
  })

  it('groups plan catalog sections by segment', () => {
    expect(buildSuitePlanCatalogSections(plans)).toEqual([
      {
        segment: 'CONSUMER',
        plans
      }
    ])
  })

  it('formats usage values and status tags', () => {
    expect(formatUsageValue(1024, 'bytes')).toBe('1.00 KB')
    expect(formatUsageValue(18, 'count')).toBe('18')
    expect(usagePercent(18, 100)).toBe(18)
    expect(suiteProductStatusTagType('ENABLED')).toBe('success')
    expect(suiteProductStatusTagType('ROADMAP')).toBe('info')
  })

  it('registers suite plans translations in all supported locales', () => {
    expect(translate(messages, 'en', 'suite.hero.badge')).toBe('Plan journey')
    expect(translate(messages, 'zh-CN', 'suite.plans.switchTo', { plan: 'Unlimited' })).toBe('切换到 Unlimited')
    expect(translate(messages, 'zh-TW', 'suite.plans.matrixTitle')).toBe('產品能力矩陣')
    expect(translate(messages, 'zh-CN', 'suite.productStatus.PREVIEW')).toBe('预览')
    expect(translate(messages, 'zh-CN', 'suite.plans.segment.BUSINESS.title')).toBe('企业计划')
    expect(translate(messages, 'zh-TW', 'suite.plans.price.perUser')).toBe('每位使用者 / 每月')
  })
})
