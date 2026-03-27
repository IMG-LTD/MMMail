import type { SuiteProductItem, SuiteProductStatus } from '../types/api'
import type { SuitePlan, SuitePlanCode, SuitePlanSegment, SuiteSubscription } from '../types/suite-lumo'

type VisibleProduct = Pick<SuiteProductItem, 'code' | 'name' | 'enabledByPlan'>
type QuotaProductCode = 'MAIL' | 'CALENDAR' | 'DRIVE'
type UsageMetricKey = 'mail' | 'contacts' | 'calendarEvents' | 'calendarShares' | 'driveStorage'
type QuotaMetricKey = 'mailDailySend' | 'contacts' | 'calendarEvents' | 'calendarShares' | 'driveStorage'

const SEGMENT_ORDER: readonly SuitePlanSegment[] = ['CONSUMER', 'BUSINESS', 'SECURITY'] as const

export interface SuitePlanUsageRow {
  key: UsageMetricKey
  count: number
  limit: number
  unit: 'count' | 'bytes'
}

export interface SuitePlanJourneyQuotaRow {
  key: QuotaMetricKey
  value: number | string
}

export interface SuitePlanCatalogSection {
  segment: SuitePlanSegment
  plans: SuitePlan[]
}

export interface SuiteUpgradeSummary {
  enabledProductCount: number
  totalProductCount: number
  availableUpgradeCodes: SuitePlanCode[]
  availableUpgradeNames: string[]
}

function listVisibleQuotaProductCodes(visibleProducts: readonly VisibleProduct[]): QuotaProductCode[] {
  const visibleCodeSet = new Set(visibleProducts.map(item => item.code))
  return ['MAIL', 'CALENDAR', 'DRIVE'].filter((code): code is QuotaProductCode => visibleCodeSet.has(code))
}

function buildUsageRowsForProduct(
  subscription: SuiteSubscription,
  productCode: QuotaProductCode
): SuitePlanUsageRow[] {
  if (productCode === 'MAIL') {
    return [
      { key: 'mail', count: subscription.usage.mailCount, limit: subscription.plan.mailDailySendLimit, unit: 'count' },
      { key: 'contacts', count: subscription.usage.contactCount, limit: subscription.plan.contactLimit, unit: 'count' }
    ]
  }
  if (productCode === 'CALENDAR') {
    return [
      {
        key: 'calendarEvents',
        count: subscription.usage.calendarEventCount,
        limit: subscription.plan.calendarEventLimit,
        unit: 'count'
      },
      {
        key: 'calendarShares',
        count: subscription.usage.calendarShareCount,
        limit: subscription.plan.calendarShareLimit,
        unit: 'count'
      }
    ]
  }
  return [
    {
      key: 'driveStorage',
      count: subscription.usage.driveStorageBytes,
      limit: subscription.plan.driveStorageMb * 1024 * 1024,
      unit: 'bytes'
    }
  ]
}

function buildQuotaRowsForProduct(plan: SuitePlan, productCode: QuotaProductCode): SuitePlanJourneyQuotaRow[] {
  if (productCode === 'MAIL') {
    return [
      { key: 'mailDailySend', value: plan.mailDailySendLimit },
      { key: 'contacts', value: plan.contactLimit }
    ]
  }
  if (productCode === 'CALENDAR') {
    return [
      { key: 'calendarEvents', value: plan.calendarEventLimit },
      { key: 'calendarShares', value: plan.calendarShareLimit }
    ]
  }
  return [{ key: 'driveStorage', value: `${plan.driveStorageMb} MB` }]
}

function createPlanNameMap(plans: readonly SuitePlan[]): Map<SuitePlanCode, string> {
  const mapping = new Map<SuitePlanCode, string>()
  for (const plan of plans) {
    mapping.set(plan.code, plan.name)
  }
  return mapping
}

export function buildSuitePlanUsageRows(
  subscription: SuiteSubscription | null,
  visibleProducts: readonly VisibleProduct[]
): SuitePlanUsageRow[] {
  if (!subscription) {
    return []
  }
  return listVisibleQuotaProductCodes(visibleProducts).flatMap(productCode => {
    return buildUsageRowsForProduct(subscription, productCode)
  })
}

export function buildSuiteJourneyQuotaRows(
  plan: SuitePlan,
  visibleProducts: readonly VisibleProduct[]
): SuitePlanJourneyQuotaRow[] {
  return listVisibleQuotaProductCodes(visibleProducts).flatMap(productCode => {
    return buildQuotaRowsForProduct(plan, productCode)
  })
}

export function buildSuitePlanCatalogSections(plans: readonly SuitePlan[]): SuitePlanCatalogSection[] {
  return SEGMENT_ORDER.map(segment => ({
    segment,
    plans: plans.filter(plan => plan.segment === segment)
  })).filter(section => section.plans.length > 0)
}

export function buildSuiteUpgradeSummary(
  subscription: SuiteSubscription | null,
  plans: readonly SuitePlan[],
  visibleProducts: readonly VisibleProduct[]
): SuiteUpgradeSummary {
  const planNameMap = createPlanNameMap(plans)
  const currentPlan = plans.find(plan => plan.code === subscription?.planCode) ?? null
  const availableUpgradeCodes = currentPlan?.upgradeTargets ?? plans
    .filter(plan => plan.code !== subscription?.planCode)
    .map(plan => plan.code)

  return {
    enabledProductCount: visibleProducts.filter(item => item.enabledByPlan).length,
    totalProductCount: visibleProducts.length,
    availableUpgradeCodes,
    availableUpgradeNames: availableUpgradeCodes.map(code => planNameMap.get(code) || code)
  }
}

export function formatBytes(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`
  }
  const units = ['KB', 'MB', 'GB', 'TB']
  let value = bytes / 1024
  let index = 0
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index += 1
  }
  return `${value.toFixed(value >= 10 ? 1 : 2)} ${units[index]}`
}

export function formatUsageValue(value: number, unit: 'count' | 'bytes'): string {
  return unit === 'bytes' ? formatBytes(value) : String(value)
}

export function usagePercent(count: number, limit: number): number {
  if (limit <= 0) {
    return 0
  }
  return Math.min(100, Math.round((count / limit) * 100))
}

export function suiteProductStatusTagType(
  status: SuiteProductStatus
): 'success' | 'warning' | 'info' {
  if (status === 'ENABLED') {
    return 'success'
  }
  if (status === 'COMING_SOON') {
    return 'warning'
  }
  return 'info'
}
