import type { ProductEnabledResolver } from '~/types/org-access'

export const COLLABORATION_PRODUCT_CODES = ['MAIL', 'CALENDAR', 'DRIVE', 'PASS'] as const

export type MainlineCollaborationProductCode = typeof COLLABORATION_PRODUCT_CODES[number]

export type CollaborationFilter = 'ALL' | MainlineCollaborationProductCode

export interface CollaborationEventLike {
  productCode: string
  sessionId?: string | null
}

export interface MainlineCollaborationEvent extends CollaborationEventLike {
  eventId: number
  eventType: string
  title: string
  summary: string
  routePath: string
  actorEmail: string | null
  createdAt: string
}

export interface CollaborationCodeLike {
  code: string
}

export interface CollaborationFilterOption {
  value: CollaborationFilter
  label: string
  count: number
}

export const COLLABORATION_PRODUCT_LABELS: Readonly<Record<MainlineCollaborationProductCode, string>> = {
  MAIL: 'Mail',
  CALENDAR: 'Calendar',
  DRIVE: 'Drive',
  PASS: 'Pass'
}

const COLLABORATION_PRODUCT_CODE_SET = new Set<string>(COLLABORATION_PRODUCT_CODES)

export function isMainlineCollaborationProductCode(
  productCode: string | null | undefined
): productCode is MainlineCollaborationProductCode {
  return Boolean(productCode && COLLABORATION_PRODUCT_CODE_SET.has(productCode))
}

export function filterMainlineCollaborationItems<T extends CollaborationEventLike>(items: readonly T[]): T[] {
  return items.filter((item) => isMainlineCollaborationProductCode(item.productCode))
}

export function filterMainlineCollaborationProducts<T extends CollaborationCodeLike>(items: readonly T[]): T[] {
  return items.filter((item) => isMainlineCollaborationProductCode(item.code))
}

export function filterCollaborationEvents<T extends CollaborationEventLike>(
  items: readonly T[],
  productFilter: CollaborationFilter
): T[] {
  const visibleItems = filterMainlineCollaborationItems(items)
  if (productFilter === 'ALL') {
    return visibleItems
  }
  return visibleItems.filter((item) => item.productCode === productFilter)
}

export function buildCollaborationCounts(items: readonly CollaborationEventLike[]): Record<string, number> {
  const counts = COLLABORATION_PRODUCT_CODES.reduce<Record<string, number>>((acc, productCode) => {
    acc[productCode] = 0
    return acc
  }, { ALL: 0 })
  for (const item of items) {
    if (!isMainlineCollaborationProductCode(item.productCode)) {
      continue
    }
    counts.ALL += 1
    counts[item.productCode] += 1
  }
  return counts
}

export function listVisibleCollaborationProductCodes(
  isEnabled: ProductEnabledResolver
): MainlineCollaborationProductCode[] {
  return COLLABORATION_PRODUCT_CODES.filter((productCode) => isEnabled(productCode))
}

export function formatCollaborationProductList(
  productCodes: readonly MainlineCollaborationProductCode[],
  mode: 'sentence' | 'slash' = 'sentence'
): string {
  const labels = productCodes.map((productCode) => COLLABORATION_PRODUCT_LABELS[productCode])
  if (labels.length === 0) {
    return 'no collaboration products'
  }
  if (mode === 'slash') {
    return labels.join(' / ')
  }
  if (labels.length === 1) {
    return labels[0]
  }
  if (labels.length === 2) {
    return `${labels[0]} and ${labels[1]}`
  }
  return `${labels.slice(0, -1).join(', ')}, and ${labels[labels.length - 1]}`
}

export function isExternalCollaborationEvent(item: CollaborationEventLike, currentSessionId: string): boolean {
  if (!item.sessionId || !currentSessionId) {
    return false
  }
  return item.sessionId !== currentSessionId
}
