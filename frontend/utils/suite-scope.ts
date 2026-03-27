import type { SuiteParityProduct, SuitePlan, SuiteSubscription } from '../types/suite-lumo'

type VisibleProduct = Pick<SuiteParityProduct, 'code' | 'name'>
type QuotaProductCode = 'MAIL' | 'CALENDAR' | 'DRIVE'

export type SuiteUsageRow = {
  label: string
  count: number
  limit: number
  unit: 'count' | 'bytes'
}

export type SuitePlanQuotaRow = {
  label: string
  value: number | string
}

const SEARCHABLE_PRODUCT_CODES = new Set([
  'AUTH',
  'AUTHENTICATOR',
  'CALENDAR',
  'DOCS',
  'DRIVE',
  'LUMO',
  'MAIL',
  'MEET',
  'PASS',
  'SHEETS',
  'SIMPLELOGIN',
  'STANDARD_NOTES',
  'WALLET'
])

function listVisibleQuotaProductCodes(visibleProducts: VisibleProduct[]): QuotaProductCode[] {
  const visibleCodeSet = new Set(visibleProducts.map(item => item.code))
  return ['MAIL', 'CALENDAR', 'DRIVE'].filter((code): code is QuotaProductCode => visibleCodeSet.has(code))
}

function formatProductList(names: string[]): string {
  if (names.length === 0) {
    return ''
  }
  if (names.length === 1) {
    return names[0]
  }
  if (names.length === 2) {
    return `${names[0]} and ${names[1]}`
  }
  return `${names.slice(0, -1).join(', ')}, and ${names[names.length - 1]}`
}

function buildUsageRowsForProduct(subscription: SuiteSubscription, productCode: QuotaProductCode): SuiteUsageRow[] {
  if (productCode === 'MAIL') {
    return [
      { label: 'Mail', count: subscription.usage.mailCount, limit: subscription.plan.mailDailySendLimit, unit: 'count' },
      { label: 'Contacts', count: subscription.usage.contactCount, limit: subscription.plan.contactLimit, unit: 'count' }
    ]
  }
  if (productCode === 'CALENDAR') {
    return [
      {
        label: 'Calendar Events',
        count: subscription.usage.calendarEventCount,
        limit: subscription.plan.calendarEventLimit,
        unit: 'count'
      },
      {
        label: 'Calendar Shares',
        count: subscription.usage.calendarShareCount,
        limit: subscription.plan.calendarShareLimit,
        unit: 'count'
      }
    ]
  }
  return [
    {
      label: 'Drive Storage',
      count: subscription.usage.driveStorageBytes,
      limit: subscription.plan.driveStorageMb * 1024 * 1024,
      unit: 'bytes'
    }
  ]
}

function buildPlanQuotaRowsForProduct(plan: SuitePlan, productCode: QuotaProductCode): SuitePlanQuotaRow[] {
  if (productCode === 'MAIL') {
    return [
      { label: 'Mail daily send', value: plan.mailDailySendLimit },
      { label: 'Contacts', value: plan.contactLimit }
    ]
  }
  if (productCode === 'CALENDAR') {
    return [
      { label: 'Calendar events', value: plan.calendarEventLimit },
      { label: 'Calendar shares', value: plan.calendarShareLimit }
    ]
  }
  return [{ label: 'Drive storage', value: `${plan.driveStorageMb} MB` }]
}

export function buildSuiteUsageRows(
  subscription: SuiteSubscription | null,
  visibleProducts: VisibleProduct[]
): SuiteUsageRow[] {
  if (!subscription) {
    return []
  }
  return listVisibleQuotaProductCodes(visibleProducts).flatMap(productCode => {
    return buildUsageRowsForProduct(subscription, productCode)
  })
}

export function shouldShowDriveEntityUsage(visibleProducts: VisibleProduct[]): boolean {
  return visibleProducts.some(item => item.code === 'DRIVE')
}

export function buildSuitePlanQuotaRows(plan: SuitePlan, visibleProducts: VisibleProduct[]): SuitePlanQuotaRow[] {
  return listVisibleQuotaProductCodes(visibleProducts).flatMap(productCode => {
    return buildPlanQuotaRowsForProduct(plan, productCode)
  })
}

export function buildSuiteCommandSearchSummary(visibleProducts: VisibleProduct[]): string {
  const visibleSearchableNames = visibleProducts
    .filter(item => SEARCHABLE_PRODUCT_CODES.has(item.code))
    .map(item => item.name)
  if (visibleSearchableNames.length === 0) {
    return 'Command search is limited by the current organization scope.'
  }
  return `Command search currently covers ${formatProductList(visibleSearchableNames)}.`
}
