import { describe, expect, it } from 'vitest'
import type { SuitePlan, SuiteSubscription } from '../types/suite-lumo'
import {
  buildSuiteCommandSearchSummary,
  buildSuitePlanQuotaRows,
  buildSuiteUsageRows,
  shouldShowDriveEntityUsage
} from '../utils/suite-scope'

const visibleProducts = [
  { code: 'DRIVE', name: 'Drive' },
  { code: 'PASS', name: 'Pass' },
  { code: 'SHEETS', name: 'Sheets' }
]

const plan: SuitePlan = {
  code: 'FREE',
  name: 'Free',
  description: 'free',
  segment: 'CONSUMER',
  priceMode: 'FREE',
  priceValue: null,
  recommended: false,
  highlights: ['Starter'],
  upgradeTargets: ['UNLIMITED'],
  mailDailySendLimit: 100,
  contactLimit: 200,
  calendarEventLimit: 300,
  calendarShareLimit: 20,
  driveStorageMb: 512,
  enabledProducts: ['MAIL', 'DRIVE', 'PASS']
}

const subscription: SuiteSubscription = {
  planCode: 'FREE',
  planName: 'Free',
  status: 'ACTIVE',
  updatedAt: '2026-03-10T10:00:00',
  usage: {
    mailCount: 18,
    contactCount: 9,
    calendarEventCount: 12,
    calendarShareCount: 4,
    driveFileCount: 20,
    driveFolderCount: 6,
    driveStorageBytes: 1024
  },
  plan
}

describe('suite scope helpers', () => {
  it('limits usage and plan quotas to visible product metrics', () => {
    const usageRows = buildSuiteUsageRows(subscription, visibleProducts)
    const quotaRows = buildSuitePlanQuotaRows(plan, visibleProducts)

    expect(usageRows.map(item => item.label)).toEqual(['Drive Storage'])
    expect(quotaRows).toEqual([{ label: 'Drive storage', value: '512 MB' }])
    expect(shouldShowDriveEntityUsage(visibleProducts)).toBe(true)
  })

  it('builds command search summary from visible searchable products only', () => {
    expect(buildSuiteCommandSearchSummary(visibleProducts)).toBe(
      'Command search currently covers Drive, Pass, and Sheets.'
    )
  })
})
