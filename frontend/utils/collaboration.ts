import type { SuiteCollaborationEvent, SuiteCollaborationProductCode } from '~/types/api'
import type { ProductEnabledResolver } from '~/types/org-access'

export const COLLABORATION_PRODUCT_CODES: readonly SuiteCollaborationProductCode[] = ['DOCS', 'DRIVE', 'SHEETS', 'MEET']

export const COLLABORATION_PRODUCT_LABELS: Readonly<Record<SuiteCollaborationProductCode, string>> = {
  DOCS: 'Docs',
  DRIVE: 'Drive',
  SHEETS: 'Sheets',
  MEET: 'Meet'
}

export function filterCollaborationEvents(
  items: SuiteCollaborationEvent[],
  productFilter: 'ALL' | SuiteCollaborationProductCode
): SuiteCollaborationEvent[] {
  if (productFilter === 'ALL') {
    return items
  }
  return items.filter((item) => item.productCode === productFilter)
}

export function buildCollaborationCounts(items: SuiteCollaborationEvent[]): Record<string, number> {
  return items.reduce<Record<string, number>>((acc, item) => {
    acc.ALL += 1
    acc[item.productCode] += 1
    return acc
  }, {
    ALL: 0,
    DOCS: 0,
    DRIVE: 0,
    SHEETS: 0,
    MEET: 0
  })
}

export function listVisibleCollaborationProductCodes(
  isEnabled: ProductEnabledResolver
): SuiteCollaborationProductCode[] {
  return COLLABORATION_PRODUCT_CODES.filter((productCode) => isEnabled(productCode))
}

export function formatCollaborationProductList(
  productCodes: readonly SuiteCollaborationProductCode[],
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

export function isExternalCollaborationEvent(item: SuiteCollaborationEvent, currentSessionId: string): boolean {
  if (!item.sessionId || !currentSessionId) {
    return false
  }
  return item.sessionId !== currentSessionId
}
