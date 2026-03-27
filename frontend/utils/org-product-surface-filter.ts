import type {
  SuiteCollaborationCenter,
  SuiteCollaborationSync,
  SuiteCommandCenter,
  SuiteCommandFeed,
  SuiteNotificationCenter,
  SuiteProductItem,
  SuiteReadinessItem,
  SuiteReadinessReport,
  SuiteRiskLevel,
  SuiteSecurityPosture,
  SuiteUnifiedSearchResult
} from '../types/api'
import type { ProductEnabledResolver } from '../types/org-access'
import type { OrgProductKey } from '../types/organization-admin'
import { buildCollaborationCounts } from './collaboration'
import { resolveProductKeyFromPath } from './org-product-access'

const PRODUCT_KEYS: OrgProductKey[] = [
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
  'VPN',
  'WALLET'
]
const SCOPE_RESTRICTED_AGGREGATE_COMMAND_EVENT_TYPES = new Set([
  'SUITE_PLAN_LIST',
  'SUITE_SUBSCRIPTION_QUERY',
  'SUITE_PRODUCT_LIST',
  'SUITE_PLAN_CHANGE',
  'SUITE_READINESS_QUERY',
  'SUITE_SECURITY_POSTURE_QUERY',
  'SUITE_GOVERNANCE_OVERVIEW_QUERY',
  'SUITE_GOVERNANCE_TEMPLATE_LIST',
  'SUITE_GOVERNANCE_CHANGE_REQUEST_LIST',
  'SUITE_COMMAND_CENTER_QUERY',
  'SUITE_COLLABORATION_CENTER_QUERY',
  'SUITE_NOTIFICATION_CENTER_QUERY',
  'SUITE_NOTIFICATION_OPERATION_HISTORY_QUERY'
])

function normalizeRoutePath(routePath?: string | null): string {
  if (!routePath) {
    return ''
  }
  return routePath.split('?')[0] || ''
}

function resolveSurfaceProductKey(productCode?: string | null, routePath?: string | null): OrgProductKey | null {
  if (productCode && PRODUCT_KEYS.includes(productCode as OrgProductKey)) {
    return productCode as OrgProductKey
  }
  return resolveProductKeyFromPath(normalizeRoutePath(routePath))
}

function isVisible(productCode: string | null | undefined, routePath: string | null | undefined, isEnabled: ProductEnabledResolver): boolean {
  const resolvedProductKey = resolveSurfaceProductKey(productCode, routePath)
  if (!resolvedProductKey) {
    return true
  }
  return isEnabled(resolvedProductKey)
}

function hasScopeRestrictions(isEnabled: ProductEnabledResolver): boolean {
  return PRODUCT_KEYS.some(productKey => !isEnabled(productKey))
}

function isRestrictedAggregateCommandEvent(
  eventType: string,
  productCode: string | null | undefined,
  routePath: string | null | undefined,
  restrictedScope: boolean
): boolean {
  if (!restrictedScope) {
    return false
  }
  if (resolveSurfaceProductKey(productCode, routePath)) {
    return false
  }
  return SCOPE_RESTRICTED_AGGREGATE_COMMAND_EVENT_TYPES.has(eventType)
}

function toRiskLevel(score: number): SuiteRiskLevel {
  if (score < 45) {
    return 'CRITICAL'
  }
  if (score < 65) {
    return 'HIGH'
  }
  if (score < 80) {
    return 'MEDIUM'
  }
  return 'LOW'
}

function filterReadinessItems(items: SuiteReadinessItem[], isEnabled: ProductEnabledResolver): SuiteReadinessItem[] {
  return items.filter(item => isVisible(item.productCode, null, isEnabled))
}

export function filterSuiteProductsByAccess(products: SuiteProductItem[], isEnabled: ProductEnabledResolver): SuiteProductItem[] {
  return products.filter(item => isVisible(item.code, null, isEnabled))
}

export function filterSuiteCommandCenterByAccess(
  commandCenter: SuiteCommandCenter | null,
  isEnabled: ProductEnabledResolver
): SuiteCommandCenter | null {
  if (!commandCenter) {
    return null
  }
  return {
    ...commandCenter,
    quickRoutes: commandCenter.quickRoutes.filter(item => isVisible(item.productCode, item.routePath, isEnabled)),
    pinnedSearches: commandCenter.pinnedSearches.filter(item => isVisible(item.productCode, item.routePath, isEnabled)),
    recentKeywords: isEnabled('MAIL') ? commandCenter.recentKeywords : [],
    recommendedActions: commandCenter.recommendedActions.filter(item => isVisible(item.productCode, null, isEnabled))
  }
}

export function filterSuiteCommandFeedByAccess(
  commandFeed: SuiteCommandFeed | null,
  isEnabled: ProductEnabledResolver
): SuiteCommandFeed | null {
  if (!commandFeed) {
    return null
  }
  const restrictedScope = hasScopeRestrictions(isEnabled)
  const items = commandFeed.items.filter((item) => {
    if (isRestrictedAggregateCommandEvent(item.eventType, item.productCode, item.routePath, restrictedScope)) {
      return false
    }
    return isVisible(item.productCode, item.routePath, isEnabled)
  })
  return {
    ...commandFeed,
    total: items.length,
    items
  }
}

export function filterSuiteUnifiedSearchByAccess(
  result: SuiteUnifiedSearchResult | null,
  isEnabled: ProductEnabledResolver
): SuiteUnifiedSearchResult | null {
  if (!result) {
    return null
  }
  const items = result.items.filter(item => isVisible(item.productCode, item.routePath, isEnabled))
  return {
    ...result,
    total: items.length,
    items
  }
}

export function filterSuiteReadinessByAccess(
  report: SuiteReadinessReport | null,
  isEnabled: ProductEnabledResolver
): SuiteReadinessReport | null {
  if (!report) {
    return null
  }
  const items = filterReadinessItems(report.items, isEnabled)
  const overallScore = items.length === 0
    ? 0
    : Math.round(items.reduce((sum, item) => sum + item.score, 0) / items.length)
  const highRiskProductCount = items.filter(item => item.riskLevel === 'HIGH').length
  const criticalRiskProductCount = items.filter(item => item.riskLevel === 'CRITICAL').length
  return {
    ...report,
    overallScore,
    overallRiskLevel: toRiskLevel(overallScore),
    highRiskProductCount,
    criticalRiskProductCount,
    items
  }
}

export function filterSuiteSecurityPostureByAccess(
  posture: SuiteSecurityPosture | null,
  isEnabled: ProductEnabledResolver
): SuiteSecurityPosture | null {
  if (!posture) {
    return null
  }
  return {
    ...posture,
    recommendedActions: posture.recommendedActions.filter(item => isVisible(item.productCode, null, isEnabled))
  }
}

export function filterSuiteNotificationCenterByAccess(
  notificationCenter: SuiteNotificationCenter | null,
  isEnabled: ProductEnabledResolver
): SuiteNotificationCenter | null {
  if (!notificationCenter) {
    return null
  }
  const items = notificationCenter.items.filter(item => isVisible(item.productCode, item.routePath, isEnabled))
  return {
    ...notificationCenter,
    total: items.length,
    criticalCount: items.filter(item => item.severity === 'CRITICAL').length,
    unreadCount: items.filter(item => !item.read).length,
    items
  }
}

export function filterSuiteCollaborationCenterByAccess(
  collaborationCenter: SuiteCollaborationCenter | null,
  isEnabled: ProductEnabledResolver
): SuiteCollaborationCenter | null {
  if (!collaborationCenter) {
    return null
  }
  const items = collaborationCenter.items.filter(item => isVisible(item.productCode, item.routePath, isEnabled))
  return {
    ...collaborationCenter,
    total: items.length,
    productCounts: buildCollaborationCounts(items),
    items
  }
}

export function filterSuiteCollaborationSyncByAccess(
  payload: SuiteCollaborationSync,
  isEnabled: ProductEnabledResolver
): SuiteCollaborationSync {
  const items = payload.items.filter(item => isVisible(item.productCode, item.routePath, isEnabled))
  return {
    ...payload,
    hasUpdates: items.length > 0,
    total: items.length,
    items
  }
}
